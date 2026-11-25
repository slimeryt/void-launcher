package com.voidlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.backdrops.layerBackdrop
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalLayerBackdrop
import com.voidlauncher.app.glass.LocalWallpaperScrollState
import com.voidlauncher.app.glass.WallpaperCrop
import kotlin.math.roundToInt

/**
 * Viewport-sized wallpaper for Haze / Kyant. Drawn at the parent size (not a
 * multi-page strip) so [layerBackdrop] never records a layer larger than the screen.
 */
@Composable
fun WallpaperHazeSource(
    modifier: Modifier = Modifier,
    attachLayer: Boolean = true
) {
    val wp = LocalBlurredWallpaper.current
    val scroll = LocalWallpaperScrollState.current
    val layerBackdrop = LocalLayerBackdrop.current
    val layerMod = if (attachLayer && layerBackdrop != null) {
        Modifier.layerBackdrop(layerBackdrop)
    } else {
        Modifier
    }

    if (wp == null) {
        Canvas(
            modifier = modifier
                .then(layerMod)
                .clipToBounds()
                .fillMaxSize()
        ) {
            drawRect(Color(0xFF05060A))
        }
        return
    }

    Canvas(
        modifier = modifier
            .then(layerMod)
            .clipToBounds()
            .fillMaxSize()
    ) {
        val extraW = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0).toFloat()
        val originX = scroll.offset.coerceIn(0f, 1f) * extraW
        val (_, originY) = WallpaperCrop.viewportOrigin(wp, wallpaperXOffset = 0.5f)
        val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat().coerceAtLeast(1f)
        val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat().coerceAtLeast(1f)
        val srcX = (originX * scaleX).roundToInt()
            .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
        val srcY = (originY * scaleY).roundToInt()
            .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
        val srcW = (size.width * scaleX).roundToInt().coerceAtLeast(1)
            .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
        val srcH = (size.height * scaleY).roundToInt().coerceAtLeast(1)
            .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))
        drawImage(
            image = wp.image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                size.width.roundToInt().coerceAtLeast(1),
                size.height.roundToInt().coerceAtLeast(1)
            )
        )
    }
}
