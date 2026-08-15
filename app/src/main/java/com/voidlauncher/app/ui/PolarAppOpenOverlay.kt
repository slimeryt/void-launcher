package com.voidlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import kotlin.math.roundToInt

/**
 * Polar-drawn icon → app window morph. Runs on every device Polar can draw.
 * System clip-reveal (when the OEM honors it) lines up with the same start rect.
 */
@Composable
fun PolarAppOpenOverlay(
    app: AppInfo,
    start: android.graphics.Rect?,
    progress: Float,
    rootPos: Offset,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val appearance = LocalIconAppearance.current
    val t = progress.coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val defaultSize = with(density) { 58.dp.toPx() }
        val sLeft = (start?.left?.toFloat() ?: (screenW / 2f - defaultSize / 2f)) - rootPos.x
        val sTop = (start?.top?.toFloat() ?: (screenH / 2f - defaultSize / 2f)) - rootPos.y
        val sW = start?.width()?.toFloat()?.coerceAtLeast(8f) ?: defaultSize
        val sH = start?.height()?.toFloat()?.coerceAtLeast(8f) ?: defaultSize
        val left = lerp(sLeft, 0f, t)
        val top = lerp(sTop, 0f, t)
        val w = lerp(sW, screenW, t)
        val h = lerp(sH, screenH, t)
        val startRadius = appearance.cornerRadiusPercent.coerceIn(8f, 50f)
        val radiusPct = lerp(startRadius, 1f, t * t).toInt().coerceIn(1, 50)
        val shape = remember(radiusPct) { SmoothCornerShape(percent = radiusPct) }
        val iconAlpha = (1f - t * 1.4f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(
                    width = with(density) { w.toDp() },
                    height = with(density) { h.toDp() }
                )
                .shadow(elevation = (18f * t).dp, shape = shape, clip = false)
                .clip(shape)
                .background(Color(0xFF0A0A0C)),
            contentAlignment = Alignment.Center
        ) {
            if (iconAlpha > 0.02f) {
                Box(Modifier.graphicsLayer { alpha = iconAlpha }) {
                    AppIcon(
                        app = app,
                        showLabel = false,
                        iconScale = appearance.scale,
                        onClick = {},
                        onLongClick = {},
                        longPressEnabled = false,
                        trackLaunchBounds = false
                    )
                }
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
