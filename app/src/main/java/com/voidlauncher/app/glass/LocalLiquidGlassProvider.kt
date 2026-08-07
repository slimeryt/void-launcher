package com.voidlauncher.app.glass

import androidx.compose.runtime.staticCompositionLocalOf
import dev.liquidglass.compose.LiquidGlassProviderState

/**
 * Optional shared [LiquidGlassProviderState] for a screen's backdrop.
 * When set, [com.voidlauncher.app.ui.components.GlassPanel] with
 * `sampleWallpaper = false` refracts this live UI instead of a flat dark plate.
 */
val LocalLiquidGlassProvider =
    staticCompositionLocalOf<LiquidGlassProviderState?> { null }
