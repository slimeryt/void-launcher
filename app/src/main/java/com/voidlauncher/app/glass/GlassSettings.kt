package com.voidlauncher.app.glass

import androidx.compose.runtime.staticCompositionLocalOf

/** Runtime liquid-glass tuning from Settings. */
data class GlassSettings(
    /** Multiplies frost / surface veil (0–1.5). */
    val frostAmount: Float = 1f,
    /** Absolute Kyant blur radius in dp (0–40). Used only when [blurEnabled]. */
    val blurRadiusDp: Float = 18f,
    /** When false, skip Kyant [com.kyant.backdrop.effects.blur] entirely. */
    val blurEnabled: Boolean = true,
    /** Kyant vibrancy color filter (the extra “built-in” frost). Off by default. */
    val vibrancyEnabled: Boolean = false,
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
