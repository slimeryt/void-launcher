package com.voidlauncher.app.glass

import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Applies Polar glass knobs 1:1. Skip [vibrancy]/[blur] when the matching
 * setting is off so refraction can exist without a baked-in frost.
 */
fun BackdropEffectScope.applyPolarGlass(
    glass: GlassSettings,
    blurScale: Float = 1f,
    lensScale: Float = 1f,
    strong: Boolean = false,
    depthEffect: Boolean = false
) {
    if (glass.vibrancyEnabled) {
        vibrancy()
    }
    if (glass.blurEnabled) {
        val radiusDp = (glass.blurRadiusDp * blurScale * if (strong) 1.08f else 1f)
            .coerceAtLeast(0f)
        if (radiusDp >= 0.25f) {
            blur(radiusDp.dp.toPx())
        }
    }
    if (glass.refractionEnabled) {
        val heightDp = glass.refractionHeightDp * lensScale * if (strong) 1.1f else 1f
        val amountDp = glass.refractionAmountDp * lensScale * if (strong) 1.1f else 1f
        if (heightDp > 0.25f && amountDp > 0.25f) {
            lens(
                refractionHeight = heightDp.dp.toPx(),
                refractionAmount = amountDp.dp.toPx(),
                depthEffect = depthEffect,
                chromaticAberration = glass.chromaticAberration
            )
        }
    }
}
