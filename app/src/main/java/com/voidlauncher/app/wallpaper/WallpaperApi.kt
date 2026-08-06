package com.voidlauncher.app.wallpaper

import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Bridges the in-app wallpaper picker to [android.app.WallpaperManager] work
 * that should run on the Activity / application context.
 */
data class WallpaperApi(
    val onSetFromUri: (Uri) -> Unit = {},
    val onSetSolidColor: (Color) -> Unit = {},
    val onSetGradient: (List<Color>) -> Unit = {},
    val onOpenSystemPicker: () -> Unit = {}
)

val LocalWallpaperApi = compositionLocalOf { WallpaperApi() }
