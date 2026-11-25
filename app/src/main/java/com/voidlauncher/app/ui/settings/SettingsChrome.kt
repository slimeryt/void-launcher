package com.voidlauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.glass.LocalLayerBackdrop
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.components.WallpaperHazeSource
import com.voidlauncher.app.ui.liquid.LiquidButton
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Dark charcoal gray used for settings cards (non-glass surfaces). */
val SettingsCardBg = Color(0xFF2C2C2E)
val SettingsChipBg = Color(0xFF3A3A3C)
val SettingsDivider = Color(0x14FFFFFF)
val SettingsCardShape = SmoothCornerShape(28.dp)

@Composable
fun SettingsHazeHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hazeState = rememberHazeState()
    val layerBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalLayerBackdrop provides layerBackdrop
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Wallpaper sibling is the Kyant capture source so glass knobs are visible.
            WallpaperHazeSource(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(layerBackdrop)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
            )
            content()
        }
    }
}

fun Modifier.settingsHazeSource(hazeState: HazeState?, zIndex: Float = 1f): Modifier =
    if (hazeState != null) hazeSource(state = hazeState, zIndex = zIndex) else this

@Composable
fun SettingsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalLayerBackdrop.current
    if (backdrop != null) {
        LiquidButton(
            onClick = onBack,
            backdrop = backdrop,
            modifier = modifier,
            surfaceColor = Color.White.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = IosBlue,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "Back",
                style = MaterialTheme.typography.titleMedium,
                color = IosBlue
            )
        }
    } else {
        GlassPanel(
            modifier = modifier,
            cornerRadius = 22.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true,
            sampleWallpaper = false
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
                    .padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = IosBlue
                )
            }
        }
    }
}

@Composable
fun SettingsBackBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsBackButton(onBack = onBack)
    }
}

@Composable
fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            cornerRadius = 24.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true,
            sampleWallpaper = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = VoidMuted,
                    modifier = Modifier.size(22.dp)
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(IosBlue),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = VoidMist),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search settings",
                                style = MaterialTheme.typography.bodyLarge,
                                color = VoidMuted
                            )
                        }
                        inner()
                    }
                )
            }
        }
    }
}
