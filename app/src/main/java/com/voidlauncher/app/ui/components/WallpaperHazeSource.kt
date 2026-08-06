package com.voidlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import kotlin.math.roundToInt

/**
 * Full-screen wallpaper drawn in Compose so Haze can blur the real backdrop
 * behind glass (system wallpaper alone isn't a Compose layer).
 */
@Composable
fun WallpaperHazeSource(modifier: Modifier = Modifier) {
    val wp = LocalBlurredWallpaper.current
    val wallpaperXOffset = LocalWallpaperXOffset.current

    Canvas(modifier = modifier.fillMaxSize()) {
        if (wp == null) {
            drawRect(Color(0xFF05060A))
            return@Canvas
        }
        val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat()
        val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat()
        val extraWidthPx = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
        val pageOffsetPx = wallpaperXOffset.coerceIn(0f, 1f) * extraWidthPx

        val srcX = (pageOffsetPx * scaleX).roundToInt()
            .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
        val srcY = 0
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
