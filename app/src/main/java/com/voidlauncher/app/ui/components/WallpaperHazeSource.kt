package com.voidlauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import com.voidlauncher.app.glass.WallpaperCrop
import kotlin.math.roundToInt

/**
 * Full-screen wallpaper drawn in Compose so Haze can blur the real backdrop
 * (system wallpaper alone isn't a Compose layer).
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
        val src = WallpaperCrop.screenSrc(
            wp = wp,
            wallpaperXOffset = wallpaperXOffset,
            screenW = size.width,
            screenH = size.height
        )
        drawImage(
            image = wp.image,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(src.w, src.h),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                size.width.roundToInt().coerceAtLeast(1),
                size.height.roundToInt().coerceAtLeast(1)
            )
        )
    }
}
