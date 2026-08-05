package com.voidlauncher.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.update.UpdateUiState
import com.voidlauncher.app.viewmodel.LauncherUiState

private val SettingsCardBg = Color(0xFF1C1F26)
private val SettingsChipBg = Color(0xFF2A2E38)
private val SettingsDivider = Color(0x14FFFFFF)
/** Continuous corners at 20dp — soft without a bubbly radius. */
private val CardShape = SmoothCornerShape(20.dp)

private enum class SettingsPage { Root, LiquidGlass, HomeLayout, Updates, General }

@Composable
fun SettingsContent(
    state: LauncherUiState,
    updateState: UpdateUiState,
    hasWallpaperAccess: Boolean,
    onGrantWallpaperAccess: () -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onDockLabelsChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onGlassFrostChange: (Float) -> Unit,
    onGlassRefractionChange: (Boolean) -> Unit,
    onGlassSheenChange: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onCancelUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(SettingsPage.Root) }

    BackHandler {
        if (page != SettingsPage.Root) page = SettingsPage.Root
        else onBack()
    }

    if (page == SettingsPage.Updates) {
        UpdatesScreen(
            updateState = updateState,
            onBack = { page = SettingsPage.Root },
            onCheckUpdate = onCheckUpdate,
            onCancelCheck = onCancelUpdate,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeader(
            title = when (page) {
                SettingsPage.Root -> "Polar"
                SettingsPage.LiquidGlass -> "Liquid Glass"
                SettingsPage.HomeLayout -> "Home Screen"
                SettingsPage.Updates -> "Updates"
                SettingsPage.General -> "General"
            },
            subtitle = when (page) {
                SettingsPage.Root -> "Launcher settings"
                else -> null
            },
            onBack = {
                if (page != SettingsPage.Root) page = SettingsPage.Root
                else onBack()
            }
        )

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == SettingsPage.Root) {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                }
            },
            label = "settings-page",
            modifier = Modifier.fillMaxSize()
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (current) {
                    SettingsPage.Root -> RootPage(
                        state = state,
                        updateState = updateState,
                        onOpen = { page = it }
                    )
                    SettingsPage.LiquidGlass -> LiquidGlassPage(
                        state = state,
                        hasWallpaperAccess = hasWallpaperAccess,
                        onGrantWallpaperAccess = onGrantWallpaperAccess,
                        onGlassBlurChange = onGlassBlurChange,
                        onGlassFrostChange = onGlassFrostChange,
                        onGlassRefractionChange = onGlassRefractionChange,
                        onGlassSheenChange = onGlassSheenChange
                    )
                    SettingsPage.HomeLayout -> HomeLayoutPage(
                        state = state,
                        onShowLabelsChange = onShowLabelsChange,
                        onDockLabelsChange = onDockLabelsChange,
                        onGridColumnsChange = onGridColumnsChange,
                        onIconScaleChange = onIconScaleChange
                    )
                    SettingsPage.Updates -> Unit
                    SettingsPage.General -> GeneralPage(
                        state = state,
                        onHapticChange = onHapticChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SettingsCardBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = VoidMist,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoidMuted
                )
            }
        }
    }
}

@Composable
private fun RootPage(
    state: LauncherUiState,
    updateState: UpdateUiState,
    onOpen: (SettingsPage) -> Unit
) {
    SettingsGroup(label = "Appearance") {
        NavRow(
            title = "Liquid Glass",
            subtitle = "Blur, frost, refraction",
            onClick = { onOpen(SettingsPage.LiquidGlass) },
            showDivider = true
        )
        NavRow(
            title = "Home Screen",
            subtitle = "Icons, labels, grid",
            onClick = { onOpen(SettingsPage.HomeLayout) },
            showDivider = false
        )
    }

    SettingsGroup(label = "System") {
        NavRow(
            title = "Updates",
            subtitle = "v${updateState.currentVersion}" +
                (updateState.available?.let { " · ${it.versionName} available" } ?: ""),
            onClick = { onOpen(SettingsPage.Updates) },
            showDivider = true
        )
        NavRow(
            title = "General",
            subtitle = "Haptics & behavior",
            onClick = { onOpen(SettingsPage.General) },
            showDivider = true
        )
        NavRow(
            title = "About",
            subtitle = "${state.apps.size} apps · ${state.hiddenCount} hidden",
            onClick = { },
            showDivider = false,
            enabled = false
        )
    }

    Text(
        text = "Swipe up on home for apps · long-press home to edit",
        style = MaterialTheme.typography.bodyMedium,
        color = VoidMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun LiquidGlassPage(
    state: LauncherUiState,
    hasWallpaperAccess: Boolean,
    onGrantWallpaperAccess: () -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onGlassFrostChange: (Float) -> Unit,
    onGlassRefractionChange: (Boolean) -> Unit,
    onGlassSheenChange: (Boolean) -> Unit
) {
    if (!hasWallpaperAccess) {
        WallpaperAccessBanner(onGrantWallpaperAccess)
    }

    // Live preview — settings apply immediately to this panel
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        cornerRadius = 28.dp,
        strong = true,
        enableSheen = state.glassSheen,
        enableRefraction = state.glassRefraction
    ) {
        Text(
            text = "Liquid Glass preview",
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    SettingsGroup(label = "Look") {
        ToggleRow(
            title = "Refraction",
            subtitle = "Lens bend + chromatic edge",
            checked = state.glassRefraction,
            onCheckedChange = onGlassRefractionChange,
            showDivider = true
        )
        ToggleRow(
            title = "Sheen",
            subtitle = "Moving highlight band",
            checked = state.glassSheen,
            onCheckedChange = onGlassSheenChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Intensity") {
        SliderBlock(
            title = "Blur strength",
            valueLabel = "${(state.glassBlurStrength * 100).toInt()}%",
            value = state.glassBlurStrength,
            onValueChange = onGlassBlurChange,
            valueRange = 0.5f..1.6f,
            showDivider = true
        )
        SliderBlock(
            title = "Frost amount",
            valueLabel = "${(state.glassFrostAmount * 100).toInt()}%",
            value = state.glassFrostAmount,
            onValueChange = onGlassFrostChange,
            valueRange = 0.4f..1.5f,
            showDivider = false
        )
    }

    Text(
        text = "Preview above updates live. Home dock / pills use the same settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = VoidMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun WallpaperAccessBanner(onGrantAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SettingsCardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Wallpaper access needed",
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist
        )
        Text(
            text = "Polar needs permission to read your wallpaper so Liquid Glass can blur " +
                "and refract it live. Without it, glass falls back to a flat look and these " +
                "settings won't visibly change anything.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted
        )
        Text(
            text = "Grant access",
            style = MaterialTheme.typography.titleMedium,
            color = IosBlue,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onGrantAccess
                )
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeLayoutPage(
    state: LauncherUiState,
    onShowLabelsChange: (Boolean) -> Unit,
    onDockLabelsChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit
) {
    SettingsGroup(label = "Labels") {
        ToggleRow(
            title = "App labels",
            subtitle = "Show names under icons",
            checked = state.showLabels,
            onCheckedChange = onShowLabelsChange,
            showDivider = true
        )
        ToggleRow(
            title = "Dock labels",
            subtitle = "Names on dock icons",
            checked = state.dockLabels,
            onCheckedChange = onDockLabelsChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Size & grid") {
        SliderBlock(
            title = "Grid columns",
            valueLabel = "${state.gridColumns}",
            value = state.gridColumns.toFloat(),
            onValueChange = { onGridColumnsChange(it.toInt()) },
            valueRange = 3f..6f,
            steps = 2,
            showDivider = true
        )
        SliderBlock(
            title = "Icon scale",
            valueLabel = "${(state.iconScale * 100).toInt()}%",
            value = state.iconScale,
            onValueChange = onIconScaleChange,
            valueRange = 0.7f..1.3f,
            showDivider = false
        )
    }
}

@Composable
private fun GeneralPage(
    state: LauncherUiState,
    onHapticChange: (Boolean) -> Unit
) {
    SettingsGroup(label = "Feedback") {
        ToggleRow(
            title = "Haptic feedback",
            subtitle = "Vibrate on long-press & edit",
            checked = state.hapticFeedback,
            onCheckedChange = onHapticChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Stats") {
        InfoRow("Apps", "${state.apps.size}", showDivider = true)
        InfoRow("Hidden", "${state.hiddenCount}", showDivider = true)
        InfoRow("Home pages", "${state.pages.size}", showDivider = false)
    }
}

@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = VoidMuted,
            modifier = Modifier.padding(start = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(SettingsCardBg)
        ) {
            content()
        }
    }
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoidMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = VoidMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 18.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VoidMuted)
            }
            IosToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun SliderBlock(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    showDivider: Boolean,
    steps: Int = 0
) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(valueLabel, style = MaterialTheme.typography.titleMedium, color = IosBlue)
            }
            Spacer(modifier = Modifier.height(12.dp))
            IosSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
            Text(value, style = MaterialTheme.typography.titleMedium, color = VoidMuted)
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
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
        !enabled -> SettingsChipBg.copy(alpha = 0.5f)
        filled -> IosBlue
        else -> SettingsChipBg
    }
    val fg = when {
        !enabled -> VoidMuted
        filled -> Color.White
        else -> VoidMist
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(SmoothCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
