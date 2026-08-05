package com.voidlauncher.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import com.voidlauncher.app.ui.settings.SettingsContent
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidTheme
import com.voidlauncher.app.update.ApkInstaller
import com.voidlauncher.app.update.UpdateViewModel
import com.voidlauncher.app.viewmodel.LauncherViewModel

/**
 * Opens like a normal app (LAUNCHER). Solid black background — not a translucent overlay.
 * Still provides wallpaper + glass locals so Liquid Glass settings preview live.
 */
class SettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    private lateinit var blurController: WallpaperBlurController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        blurController = WallpaperBlurController(applicationContext, lifecycleScope)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()
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
                )
            ) {
                VoidTheme {
                    SettingsContent(
                        state = state,
                        updateState = updateState,
                        onShowLabelsChange = viewModel::setShowLabels,
                        onGridColumnsChange = viewModel::setGridColumns,
                        onIconScaleChange = viewModel::setIconScale,
                        onDockLabelsChange = viewModel::setDockLabels,
                        onHapticChange = viewModel::setHapticFeedback,
                        onAutoCheckUpdatesChange = viewModel::setAutoCheckUpdates,
                        onGlassBlurChange = viewModel::setGlassBlurStrength,
                        onGlassFrostChange = viewModel::setGlassFrostAmount,
                        onGlassRefractionChange = viewModel::setGlassRefraction,
                        onGlassSheenChange = viewModel::setGlassSheen,
                        onCheckUpdate = { updateViewModel.checkForUpdates(silent = false) },
                        onCancelUpdate = updateViewModel::cancelUpdateAction,
                        onDownloadUpdate = updateViewModel::downloadUpdate,
                        onInstallUpdate = {
                            val apk = updateViewModel.state.value.downloadedApk
                            if (apk == null) return@SettingsContent
                            if (!ApkInstaller.canInstallPackages(this)) {
                                Toast.makeText(
                                    this,
                                    "Allow Void to install updates",
                                    Toast.LENGTH_LONG
                                ).show()
                                startActivity(ApkInstaller.installPermissionSettingsIntent(this))
                                return@SettingsContent
                            }
                            ApkInstaller.installApk(this, apk)
                        },
                        onBack = { finish() },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VoidInk)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::blurController.isInitialized) {
            blurController.refresh()
        }
    }
}
