package com.voidlauncher.app

import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.voidlauncher.app.glass.GlassSettings
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.WallpaperBlurController
import com.voidlauncher.app.ui.LauncherRoot
import com.voidlauncher.app.ui.theme.VoidTheme
import com.voidlauncher.app.util.LauncherWindow
import com.voidlauncher.app.viewmodel.LauncherViewModel
import com.voidlauncher.app.wallpaper.LocalWallpaperApi
import com.voidlauncher.app.wallpaper.WallpaperApi
import com.voidlauncher.app.widget.LocalWidgetHostApi
import com.voidlauncher.app.widget.WidgetHostApi
import kotlin.math.max

private const val WIDGET_HOST_ID = 1988

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var blurController: WallpaperBlurController

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var pendingWidgetId: Int = -1
    private var pendingProvider: AppWidgetProviderInfo? = null

    private val bindWidgetLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleBindResult(result)
        }
    private val configureWidgetLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pendingWidgetId
            pendingWidgetId = -1
            pendingProvider = null
            if (result.resultCode == RESULT_OK && id != -1) {
                viewModel.addWidget(id)
            } else if (id != -1) {
                appWidgetHost.deleteAppWidgetId(id)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableSystemWindowBlur()

        LauncherWindow.decorView = window.decorView

        blurController = WallpaperBlurController(applicationContext, lifecycleScope)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val blurredWallpaper by blurController.wallpaper.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                blurController.start()
                onDispose { blurController.stop() }
            }

            CompositionLocalProvider(
                LocalBlurredWallpaper provides blurredWallpaper,
                LocalGlassSettings provides GlassSettings(
                    blurStrength = state.glassBlurStrength,
                    frostAmount = state.glassFrostAmount,
                    refractionEnabled = state.glassRefraction,
                    sheenEnabled = state.glassSheen
                ),
                LocalWidgetHostApi provides WidgetHostApi(
                    host = appWidgetHost,
                    manager = appWidgetManager,
                    onBindProvider = { bindProvider(it) },
                    onRemoveWidget = { id ->
                        viewModel.removeWidget(id)
                        appWidgetHost.deleteAppWidgetId(id)
                    }
                ),
                LocalWallpaperApi provides WallpaperApi(
                    onSetFromUri = { setWallpaperFromUri(it) },
                    onSetBitmap = { applyWallpaperBitmap(it) },
                    onSetSolidColor = { setWallpaperColor(it) },
                    onSetGradient = { setWallpaperGradient(it) },
                    onOpenSystemPicker = {
                        runCatching {
                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SET_WALLPAPER),
                                    "System wallpapers"
                                )
                            )
                        }
                    }
                )
            ) {
                VoidTheme {
                    LauncherRoot(
                        state = state,
                        onLaunchApp = viewModel::launchApp,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onHideApp = viewModel::hideApp,
                        onAppInfo = viewModel::openAppInfo,
                        onAddAppToHome = { viewModel.addAppToHome(it) },
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onDrawerOpenChange = { viewModel.setDrawerOpen(it) },
                        onOpenDrawerSearch = viewModel::openDrawerSearch,
                        onEditModeChange = viewModel::setEditMode,
                        onRemoveHomeItem = viewModel::removeItemFromHome,
                        onSwapHomeItems = viewModel::swapItems,
                        onCreateFolder = viewModel::createFolderFromDrop,
                        onAddAppToFolder = viewModel::addAppToFolderFromDrop,
                        onAddPage = viewModel::addPage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
        if (::blurController.isInitialized) {
            blurController.refresh()
        }
        if (::appWidgetHost.isInitialized) {
            runCatching { appWidgetHost.startListening() }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::appWidgetHost.isInitialized) {
            runCatching { appWidgetHost.stopListening() }
        }
    }

    private fun bindProvider(info: AppWidgetProviderInfo) {
        val id = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        pendingProvider = info
        val allowed = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        }.getOrDefault(false)

        if (allowed) {
            proceedAfterBind(id, info)
            return
        }

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
        runCatching { bindWidgetLauncher.launch(intent) }
            .onFailure {
                appWidgetHost.deleteAppWidgetId(id)
                pendingWidgetId = -1
                pendingProvider = null
                Toast.makeText(this, "Could not bind widget", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleBindResult(result: ActivityResult) {
        val id = pendingWidgetId
        val info = pendingProvider
        if (result.resultCode != RESULT_OK || id == -1 || info == null) {
            if (id != -1) appWidgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
            pendingProvider = null
            return
        }
        proceedAfterBind(id, info)
    }

    private fun proceedAfterBind(id: Int, info: AppWidgetProviderInfo) {
        val configure = info.configure
        if (configure != null) {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            runCatching { configureWidgetLauncher.launch(configIntent) }
                .onFailure {
                    pendingWidgetId = -1
                    pendingProvider = null
                    viewModel.addWidget(id)
                }
        } else {
            pendingWidgetId = -1
            pendingProvider = null
            viewModel.addWidget(id)
        }
    }

    private fun setWallpaperFromUri(uri: Uri) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                val decoded = BitmapFactory.decodeStream(input)
                    ?: error("Could not decode image")
                applyWallpaperBitmap(decoded)
            } ?: error("Could not open image")
        }.onFailure {
            Toast.makeText(this, "Wallpaper failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setWallpaperColor(color: Color) {
        runCatching {
            val (w, h) = wallpaperSize()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(color.toArgb())
            applyWallpaperBitmap(bmp)
        }.onFailure {
            Toast.makeText(this, "Wallpaper failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setWallpaperGradient(colors: List<Color>) {
        if (colors.isEmpty()) return
        if (colors.size == 1) {
            setWallpaperColor(colors.first())
            return
        }
        runCatching {
            val (w, h) = wallpaperSize()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val stops = colors.map { it.toArgb() }.toIntArray()
            paint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                stops, null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            applyWallpaperBitmap(bmp)
        }.onFailure {
            Toast.makeText(this, "Wallpaper failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyWallpaperBitmap(bitmap: Bitmap) {
        val wm = WallpaperManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        } else {
            @Suppress("DEPRECATION")
            wm.setBitmap(bitmap)
        }
        blurController.refresh()
        Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show()
    }

    private fun wallpaperSize(): Pair<Int, Int> {
        val dm = resources.displayMetrics
        val w = max(dm.widthPixels, 1080)
        val h = max(dm.heightPixels, 1920)
        // Slightly wider than one screen so parallax still has room to breathe.
        return (w * 2) to h
    }

    private fun enableSystemWindowBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching { window.setBackgroundBlurRadius(60) }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val s = viewModel.state.value
        when {
            s.isDrawerOpen -> viewModel.setDrawerOpen(false)
            s.isEditMode -> viewModel.setEditMode(false)
            else -> { /* stay on home */ }
        }
    }
}
