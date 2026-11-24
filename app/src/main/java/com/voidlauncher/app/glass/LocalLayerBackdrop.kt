package com.voidlauncher.app.glass

import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * Shared Kyant [LayerBackdrop] so [com.voidlauncher.app.ui.components.GlassPanel]
 * can refract real Compose content (wallpaper / settings plate) behind glass.
 */
val LocalLayerBackdrop = compositionLocalOf<LayerBackdrop?> { null }
