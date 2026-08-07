package com.voidlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalWallpaperScrollState
import com.voidlauncher.app.glass.WallpaperCrop
import kotlin.math.roundToInt

/**
 * Full-screen wallpaper drawn in Compose so Haze can blur the real backdrop
 * (system wallpaper alone isn't a Compose layer).
 *
 * Parallax uses translationX on a full-width strip so scroll stays cheap.
 */
@Composable
fun WallpaperHazeSource(modifier: Modifier = Modifier) {
    val wp = LocalBlurredWallpaper.current
    val scroll = LocalWallpaperScrollState.current
    val density = LocalDensity.current

    if (wp == null) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(Color(0xFF05060A))
        }
        return
    }

    val extraW = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0).toFloat()
    val stripW = wp.fullWidthPx.toFloat().coerceAtLeast(1f)
    val stripH = wp.screenHeight.toFloat().coerceAtLeast(1f)
    val stripDp = with(density) { DpSize(stripW.toDp(), stripH.toDp()) }

    Canvas(
        modifier = modifier
            .requiredSize(stripDp)
            .graphicsLayer {
                val originX = scroll.offset.coerceIn(0f, 1f) * extraW
                translationX = -originX
            }
    ) {
        val (_, originY) = WallpaperCrop.viewportOrigin(wp, wallpaperXOffset = 0.5f)
        val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat().coerceAtLeast(1f)
        val srcY = (originY * scaleY).roundToInt()
            .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
        val srcH = (stripH * scaleY).roundToInt().coerceAtLeast(1)
            .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))
        drawImage(
            image = wp.image,
            srcOffset = IntOffset(0, srcY),
            srcSize = IntSize(wp.bitmapWidth.coerceAtLeast(1), srcH),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                stripW.roundToInt().coerceAtLeast(1),
                stripH.roundToInt().coerceAtLeast(1)
            )
        )
    }
}
