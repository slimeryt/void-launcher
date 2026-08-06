package com.voidlauncher.app

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.voidlauncher.app.widget.LocalWidgetHostApi
import com.voidlauncher.app.widget.WidgetHostApi
import java.util.ArrayList

private const val WIDGET_HOST_ID = 1988

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var blurController: WallpaperBlurController

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var pendingWidgetId: Int = -1

    private val pickWidgetLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handlePickResult(result)
        }
    private val configureWidgetLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pendingWidgetId
            pendingWidgetId = -1
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
                    onAddWidget = { startAddWidgetFlow() },
                    onRemoveWidget = { id ->
                        viewModel.removeWidget(id)
                        appWidgetHost.deleteAppWidgetId(id)
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

    private fun startAddWidgetFlow() {
        val id = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            // A handful of OEM widget pickers NPE without these, even when empty.
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList())
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList())
        }
        runCatching { pickWidgetLauncher.launch(intent) }
            .onFailure {
                appWidgetHost.deleteAppWidgetId(id)
                pendingWidgetId = -1
            }
    }

    private fun handlePickResult(result: ActivityResult) {
        val id = pendingWidgetId
        if (result.resultCode != RESULT_OK) {
            if (id != -1) appWidgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
            return
        }
        val pickedId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (pickedId == -1) {
            if (id != -1) appWidgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
            return
        }
        pendingWidgetId = pickedId
        val info = appWidgetManager.getAppWidgetInfo(pickedId)
        if (info == null) {
            appWidgetHost.deleteAppWidgetId(pickedId)
            pendingWidgetId = -1
            return
        }
        proceedAfterPick(pickedId, info)
    }

    private fun proceedAfterPick(id: Int, info: AppWidgetProviderInfo) {
        val configure = info.configure
        if (configure != null) {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            runCatching { configureWidgetLauncher.launch(configIntent) }
                .onFailure {
                    pendingWidgetId = -1
                    viewModel.addWidget(id)
                }
        } else {
            pendingWidgetId = -1
            viewModel.addWidget(id)
        }
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
