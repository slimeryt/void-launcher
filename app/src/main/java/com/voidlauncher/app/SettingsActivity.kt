package com.voidlauncher.app

import android.content.Intent
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
import com.voidlauncher.app.account.AccountViewModel
import com.voidlauncher.app.glass.GlassSettings
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.StoragePermission
import com.voidlauncher.app.glass.WallpaperBlurController
import com.voidlauncher.app.ui.settings.SettingsContent
import com.voidlauncher.app.ui.settings.SettingsHazeHost
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
    private val accountViewModel: AccountViewModel by viewModels()
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
            val accountState by accountViewModel.state.collectAsStateWithLifecycle()
            val blurredWallpaper by blurController.wallpaper.collectAsStateWithLifecycle()
            val hasWallpaperAccess by blurController.hasAccess.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                blurController.start()
                onDispose { blurController.stop() }
            }

            CompositionLocalProvider(
                LocalBlurredWallpaper provides blurredWallpaper,
                LocalGlassSettings provides GlassSettings(
                    blurStrength = state.glassBlurStrength,
                    frostAmount = state.glassFrostAmount,
                    refractionEnabled = state.glassRefraction && !state.reduceTransparency,
                    sheenEnabled = state.glassSheen && !state.reduceTransparency
                )
            ) {
                VoidTheme {
                    SettingsHazeHost(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SettingsContent(
                            state = state,
                            updateState = updateState,
                            accountState = accountState,
                            hasWallpaperAccess = hasWallpaperAccess,
                            onGrantWallpaperAccess = { startActivity(StoragePermission.requestIntent(this)) },
                            onShowLabelsChange = viewModel::setShowLabels,
                            onGridColumnsChange = viewModel::setGridColumns,
                            onIconScaleChange = viewModel::setIconScale,
                            onDockLabelsChange = viewModel::setDockLabels,
                            onHapticChange = viewModel::setHapticFeedback,
                            onShowHomeSearchChange = viewModel::setShowHomeSearch,
                            onShowAssistantChange = viewModel::setShowAssistant,
                            onShowBatteryPercentChange = viewModel::setShowBatteryPercent,
                            onReduceMotionChange = viewModel::setReduceMotion,
                            onReduceTransparencyChange = viewModel::setReduceTransparency,
                            onCustomAppAnimationsChange = viewModel::setCustomAppAnimations,
                            onUnhideApp = viewModel::unhideApp,
                            onAutoCheckUpdatesChange = viewModel::setAutoCheckUpdates,
                            onGlassBlurChange = viewModel::setGlassBlurStrength,
                            onGlassFrostChange = viewModel::setGlassFrostAmount,
                            onGlassRefractionChange = viewModel::setGlassRefraction,
                            onGlassSheenChange = viewModel::setGlassSheen,
                            onCheckUpdate = { updateViewModel.checkForUpdates(silent = false) },
                            onCancelUpdate = updateViewModel::cancelUpdateAction,
                            onDownloadUpdate = updateViewModel::downloadUpdate,
                            onUpdateChannelChange = updateViewModel::setUpdateChannel,
                            onMarkBetaChannelAgreed = updateViewModel::markBetaChannelAgreed,
                            onAccountLogin = accountViewModel::login,
                            onAccountRegister = accountViewModel::register,
                            onAccountLogout = accountViewModel::logout,
                            onRequestDeveloperAccount = accountViewModel::requestDeveloperAccount,
                            onRequestEnroll = accountViewModel::requestDeveloperEnrollment,
                            onAccountRefresh = accountViewModel::refresh,
                            onInstallUpdate = {
                                val apk = updateViewModel.state.value.downloadedApk
                                if (apk == null) return@SettingsContent
                                val block = ApkInstaller.updateBlockReason(this, apk)
                                if (block != null) {
                                    Toast.makeText(this, block, Toast.LENGTH_LONG).show()
                                    return@SettingsContent
                                }
                                if (!ApkInstaller.canInstallPackages(this)) {
                                    Toast.makeText(
                                        this,
                                        "Allow Polar to install updates",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    startActivity(ApkInstaller.installPermissionSettingsIntent(this))
                                    return@SettingsContent
                                }
                                ApkInstaller.installApk(this, apk)
                            },
                            onBack = { finish() },
                            onOpenAppIcons = {
                                startActivity(
                                    Intent(this, MainActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        )
                                        putExtra(MainActivity.EXTRA_OPEN_ICON_EDITOR, true)
                                    }
                                )
                                finish()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::blurController.isInitialized) {
            blurController.refresh()
        }
        accountViewModel.refresh()
    }
}
