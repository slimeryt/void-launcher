package com.voidlauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.kyant.backdrop.backdrops.layerBackdrop
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalLayerBackdrop
import com.voidlauncher.app.glass.LocalWallpaperScrollState

/**
 * Viewport-sized wallpaper for Haze / Kyant. The bitmap is cropped to the
 * current page (not a multi-page strip) so [layerBackdrop] stays screen-sized.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(layerMod)
    ) {
        if (wp == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF05060A))
            )
        } else {
            Image(
                bitmap = wp.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(
                    horizontalBias = scroll.offset.coerceIn(0f, 1f) * 2f - 1f,
                    verticalBias = 0f
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
        }
    }
}
