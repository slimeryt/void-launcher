package com.voidlauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.GlassPanel
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            GlassPanel(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                cornerRadius = 22.dp,
                strong = true,
                enableSheen = false,
                enableRefraction = false
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = VoidMist,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Void",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VoidMist
                )
                Text(
                    text = "Launcher settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoidMuted
                )
            }
        }

        UpdateCard(
            updateState = updateState,
            onCheckUpdate = onCheckUpdate,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate
        )

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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${state.apps.size} apps · ${state.hiddenCount} hidden",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(start = 8.dp)
        )

        Text(
            text = "Swipe up on home for apps · swipe down in apps to close · long-press home to edit",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
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
                    text = updateState.error
                        ?: updateState.statusMessage.ifBlank { "Auto-checks GitHub Releases" },
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
                    .clip(CircleShape),
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

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "If install says “package conflicts”, uninstall Void once, then install this build. Later updates will work over-the-air.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted
        )
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
    GlassPanel(
        modifier = modifier
            .height(44.dp)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 14.dp,
        strong = true,
        enableSheen = false,
        enableRefraction = false,
        tint = when {
            !enabled -> Color(0x33000000)
            filled -> VoidCyan.copy(alpha = 0.55f)
            else -> Color.Transparent
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    !enabled -> VoidMuted
                    filled -> VoidInk
                    else -> VoidMist
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
}
