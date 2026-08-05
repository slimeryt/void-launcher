package com.voidlauncher.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * iOS-style continuous corners.
 * Uses a superellipse (n≈5) which reads as a rounded square — not a circle (n=2).
 */
class SquircleShape(
    private val exponent: Float = 5.2f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rx = w / 2f
        val ry = h / 2f
        val n = exponent.coerceAtLeast(2.5f)
        val steps = 72
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (Math.PI * 2).toFloat()
            val cosT = cos(t.toDouble()).toFloat()
            val sinT = sin(t.toDouble()).toFloat()
            // Superellipse: higher n → squarer (iOS icons ≈ 5)
            val x = cx + rx * signPow(cosT, 2f / n)
            val y = cy + ry * signPow(sinT, 2f / n)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private fun signPow(v: Float, p: Float): Float {
        val s = if (v < 0f) -1f else 1f
        return s * abs(v).toDouble().pow(p.toDouble()).toFloat()
    }
}

/** Primary mask for home/dock icons — visibly squircle, not CircleShape. */
val IosSquircle: Shape = SquircleShape(exponent = 5.2f)

/** Fallback continuous-corner approx (~22% like iOS). */
val IosContinuousCorner = RoundedCornerShape(percent = 22)
