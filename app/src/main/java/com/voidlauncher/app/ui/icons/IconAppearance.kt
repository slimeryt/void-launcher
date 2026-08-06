package com.voidlauncher.app.ui.icons

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter

enum class IconTheme(val key: String, val label: String) {
    Standard("standard", "Standard"),
    Dark("dark", "Dark"),
    Tinted("tinted", "Tinted");

    companion object {
        fun fromKey(key: String): IconTheme =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Standard
    }
}

/**
 * Visual style applied to home / dock / drawer icons.
 * [cornerRadiusPercent] is 0–50 (% of half the icon side → 50% = circle).
 */
data class IconAppearance(
    val theme: IconTheme = IconTheme.Standard,
    val cornerRadiusPercent: Float = 24f,
    val tintHue: Float = 210f,
    val tintAlpha: Float = 0.55f,
    val scale: Float = 1f
) {
    val cornerRadiusRatio: Float
        get() = (cornerRadiusPercent / 100f).coerceIn(0f, 0.5f)

    val shapePercent: Int
        get() = cornerRadiusPercent.toInt().coerceIn(0, 50)

    fun colorFilter(): ColorFilter? = when (theme) {
        IconTheme.Standard -> null
        IconTheme.Dark -> ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    0.55f, 0f, 0f, 0f, 0f,
                    0f, 0.55f, 0f, 0f, 0f,
                    0f, 0f, 0.55f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        IconTheme.Tinted -> {
            val c = Color.hsv(
                hue = tintHue.mod(360f),
                saturation = 0.72f,
                value = 1f,
                alpha = tintAlpha.coerceIn(0f, 1f)
            )
            ColorFilter.tint(c, BlendMode.SrcAtop)
        }
    }

    companion object {
        val Default = IconAppearance()
    }
}

val LocalIconAppearance = staticCompositionLocalOf { IconAppearance.Default }
