package com.voidlauncher.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidlauncher.app.ui.settings.SettingsContent
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidTheme
import com.voidlauncher.app.update.ApkInstaller
import com.voidlauncher.app.update.UpdateViewModel
import com.voidlauncher.app.viewmodel.LauncherViewModel

/**
 * Opens like a normal app (LAUNCHER). Solid black background — not a translucent overlay.
 */
class SettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()

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
