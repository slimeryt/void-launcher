package com.voidlauncher.app.glass

import kotlin.math.roundToInt

/**
 * Maps a screen/panel rect into the blurred wallpaper buffer using the same
 * 0..1 offset space as [android.app.WallpaperManager.setWallpaperOffsets].
 */
object WallpaperCrop {

    data class SrcRect(val x: Int, val y: Int, val w: Int, val h: Int)

    /** Top-left of the current screen viewport inside full wallpaper pixel space. */
    fun viewportOrigin(
        wp: BlurredWallpaper,
        wallpaperXOffset: Float,
        wallpaperYOffset: Float = 0.5f
    ): Pair<Float, Float> {
        val extraW = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
        val extraH = (wp.fullHeightPx - wp.screenHeight).coerceAtLeast(0)
        val originX = wallpaperXOffset.coerceIn(0f, 1f) * extraW
        val originY = wallpaperYOffset.coerceIn(0f, 1f) * extraH
        return originX to originY
    }

    fun panelSrc(
        wp: BlurredWallpaper,
        wallpaperXOffset: Float,
        panelX: Float,
        panelY: Float,
        panelW: Float,
        panelH: Float,
        pad: Float = 0f,
        wallpaperYOffset: Float = 0.5f
    ): SrcRect {
        val (originX, originY) = viewportOrigin(wp, wallpaperXOffset, wallpaperYOffset)
        val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat().coerceAtLeast(1f)
        val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat().coerceAtLeast(1f)
        val realX = originX + panelX - panelW * pad
        val realY = originY + panelY - panelH * pad
        val srcX = (realX * scaleX).roundToInt()
            .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
        val srcY = (realY * scaleY).roundToInt()
            .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
        val srcW = ((panelW * (1f + pad * 2f)) * scaleX).roundToInt().coerceAtLeast(1)
            .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
        val srcH = ((panelH * (1f + pad * 2f)) * scaleY).roundToInt().coerceAtLeast(1)
            .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))
        return SrcRect(srcX, srcY, srcW, srcH)
    }

    fun screenSrc(
        wp: BlurredWallpaper,
        wallpaperXOffset: Float,
        screenW: Float,
        screenH: Float,
        wallpaperYOffset: Float = 0.5f
    ): SrcRect = panelSrc(
        wp = wp,
        wallpaperXOffset = wallpaperXOffset,
        panelX = 0f,
        panelY = 0f,
        panelW = screenW,
        panelH = screenH,
        pad = 0f,
        wallpaperYOffset = wallpaperYOffset
    )
}
