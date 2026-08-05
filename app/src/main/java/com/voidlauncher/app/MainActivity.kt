package com.voidlauncher.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.WallpaperBlurController
import com.voidlauncher.app.ui.LauncherRoot
import com.voidlauncher.app.ui.theme.VoidTheme
import com.voidlauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var blurController: WallpaperBlurController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableSystemWindowBlur()

        blurController = WallpaperBlurController(applicationContext, lifecycleScope)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val blurredWallpaper by blurController.wallpaper.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                blurController.start()
                onDispose { blurController.stop() }
            }

            CompositionLocalProvider(LocalBlurredWallpaper provides blurredWallpaper) {
                VoidTheme {
                    LauncherRoot(
                        state = state,
                        onLaunchApp = viewModel::launchApp,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onHideApp = viewModel::hideApp,
                        onAppInfo = viewModel::openAppInfo,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onDrawerOpenChange = viewModel::setDrawerOpen,
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
    }

    private fun enableSystemWindowBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching { window.setBackgroundBlurRadius(60) }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.state.value.isDrawerOpen) {
            viewModel.setDrawerOpen(false)
            return
        }
    }
}
