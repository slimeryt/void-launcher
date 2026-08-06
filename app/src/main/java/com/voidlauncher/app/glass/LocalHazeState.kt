package com.voidlauncher.app.glass

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * Shared [HazeState] so [com.voidlauncher.app.ui.components.GlassPanel] can blur/refract
 * real Compose content behind it (not a wallpaper portal).
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }
