package com.voidlauncher.app.ui.settings

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Modest corner size with continuous smoothing (squircle-like blend),
 * not a large circular radius.
 */
class SmoothCornerShape(
    private val radius: Dp = 14.dp,
    private val smoothing: Float = 0.8f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { radius.toPx() }
            .coerceAtMost(min(size.width, size.height) / 2f)
        return Outline.Generic(smoothRoundRect(size.width, size.height, r, smoothing.coerceIn(0f, 1f)))
    }
}

/**
 * Continuous-corner round rect: edges stay straight longer, then ease into the corner
 * (Figma-style corner smoothing), so small radii still look soft — not bubbly.
 */
private fun smoothRoundRect(w: Float, h: Float, radius: Float, smoothing: Float): Path {
    val path = Path()
    if (radius < 0.5f) {
        path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        return path
    }

    // Distance along each edge before the curve starts (higher smoothing → longer straight edge)
    val straight = radius * (1f - 0.45f * smoothing)
    // Bezier handle pull into the corner
    val handle = radius * (0.45f + 0.35f * smoothing)

    fun topRight() {
        path.cubicTo(w - straight + handle, 0f, w, straight - handle, w, straight)
    }
    fun bottomRight() {
        path.cubicTo(w, h - straight + handle, w - straight + handle, h, w - straight, h)
    }
    fun bottomLeft() {
        path.cubicTo(straight - handle, h, 0f, h - straight + handle, 0f, h - straight)
    }
    fun topLeft() {
        path.cubicTo(0f, straight - handle, straight - handle, 0f, straight, 0f)
    }

    path.moveTo(straight, 0f)
    path.lineTo(w - straight, 0f)
    topRight()
    path.lineTo(w, h - straight)
    bottomRight()
    path.lineTo(straight, h)
    bottomLeft()
    path.lineTo(0f, straight)
    topLeft()
    path.close()
    return path
}
