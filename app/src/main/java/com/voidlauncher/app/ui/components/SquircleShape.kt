package com.voidlauncher.app.ui.components

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
 * iOS-style continuous-corner / squircle (superellipse n≈5).
 */
class SquircleShape(
    private val exponent: Float = 5f
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
        val n = exponent
        val steps = 64
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (Math.PI * 2).toFloat()
            val cosT = cos(t.toDouble()).toFloat()
            val sinT = sin(t.toDouble()).toFloat()
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

val IosSquircle = SquircleShape(exponent = 5f)
