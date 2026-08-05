package com.voidlauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.update.UpdateUiState
import com.voidlauncher.app.viewmodel.LauncherUiState

@Composable
fun SettingsContent(
    state: LauncherUiState,
    updateState: UpdateUiState,
    onShowLabelsChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = VoidMist
                )
            }
            Text(
                text = "Void",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist
            )
        }

        Text(
            text = "Launcher settings",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(start = 12.dp, bottom = 20.dp)
        )

        UpdateCard(
            updateState = updateState,
            onCheckUpdate = onCheckUpdate,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(title = "Show app labels") {
            Switch(
                checked = state.showLabels,
                onCheckedChange = onShowLabelsChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidInk,
                    checkedTrackColor = VoidCyan,
                    uncheckedThumbColor = VoidMist,
                    uncheckedTrackColor = VoidMuted.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(title = "Grid columns — ${state.gridColumns}") {
            Slider(
                value = state.gridColumns.toFloat(),
                onValueChange = { onGridColumnsChange(it.toInt()) },
                valueRange = 3f..6f,
                steps = 2,
                colors = SliderDefaults.colors(
                    thumbColor = VoidCyan,
                    activeTrackColor = VoidCyan,
                    inactiveTrackColor = VoidMuted.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(title = "Icon scale — ${"%.0f".format(state.iconScale * 100)}%") {
            Slider(
                value = state.iconScale,
                onValueChange = onIconScaleChange,
                valueRange = 0.7f..1.3f,
                colors = SliderDefaults.colors(
                    thumbColor = VoidCyan,
                    activeTrackColor = VoidCyan,
                    inactiveTrackColor = VoidMuted.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${state.apps.size} apps · ${state.hiddenCount} hidden",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Swipe up on home for apps · swipe down in apps to close · long-press home to edit",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun UpdateCard(
    updateState: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    SettingCard(title = "Updates") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = VoidCyan
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Installed v${updateState.currentVersion}",
                    style = MaterialTheme.typography.titleMedium,
                    color = VoidMist
                )
                Text(
                    text = updateState.error ?: updateState.statusMessage.ifBlank { "Auto-checks GitHub Releases" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (updateState.error != null) Color(0xFFF87171) else VoidMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (updateState.checking) {
                CircularProgressIndicator(
                    color = VoidCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(22.dp)
                )
            }
        }

        if (updateState.downloading) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { updateState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp)),
                color = VoidCyan,
                trackColor = VoidMuted.copy(alpha = 0.2f)
            )
            Text(
                text = "${(updateState.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = VoidMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip(
                label = "Check",
                enabled = !updateState.checking && !updateState.downloading,
                onClick = onCheckUpdate,
                modifier = Modifier.weight(1f)
            )

            when {
                updateState.downloadedApk != null -> {
                    ActionChip(
                        label = "Install",
                        enabled = true,
                        filled = true,
                        onClick = onInstallUpdate,
                        modifier = Modifier.weight(1f)
                    )
                }
                updateState.available != null -> {
                    ActionChip(
                        label = "Download",
                        enabled = !updateState.downloading,
                        filled = true,
                        onClick = onDownloadUpdate,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    ActionChip(
                        label = "Up to date",
                        enabled = false,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        updateState.available?.let { release ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Latest: v${release.versionName} (${release.tagName})",
                style = MaterialTheme.typography.bodyMedium,
                color = VoidCyan
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val bg = when {
        !enabled -> Color(0xFF1A1D24)
        filled -> VoidCyan
        else -> Color(0xFF1C212B)
    }
    val fg = when {
        !enabled -> VoidMuted
        filled -> VoidInk
        else -> VoidMist
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF12141A))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
