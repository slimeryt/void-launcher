package com.voidlauncher.app.glass

import androidx.compose.runtime.staticCompositionLocalOf

/** Runtime liquid-glass tuning from Settings. */
data class GlassSettings(
    /** Multiplies frost / surface veil (0–1.5). */
    val frostAmount: Float = 1f,
    /** Absolute Kyant blur radius in dp (0–40). */
    val blurRadiusDp: Float = 18f,
    /** Lens edge height in dp (0–40). */
    val refractionHeightDp: Float = 16f,
    /** Lens bend amount in dp (0–80). */
    val refractionAmountDp: Float = 32f,
    val refractionEnabled: Boolean = true,
    val sheenEnabled: Boolean = true,
    val chromaticAberration: Boolean = true,
    /**
     * Legacy strength multiplier kept for older prefs / CC overrides.
     * Prefer [blurRadiusDp] for the absolute radius.
     */
    val blurStrength: Float = 1f
)

val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }
