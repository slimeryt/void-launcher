package com.voidlauncher.app.glass

import androidx.compose.runtime.staticCompositionLocalOf

/** Runtime liquid-glass tuning from Settings. */
data class GlassSettings(
    /** Multiplies GPU/stack blur (0.5–1.6). */
    val blurStrength: Float = 1f,
    /** Multiplies frost veil / shader frost (0.4–1.5). */
    val frostAmount: Float = 1f,
    val refractionEnabled: Boolean = true,
    val sheenEnabled: Boolean = true
)

val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }
