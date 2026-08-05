package com.voidlauncher.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Forced home/dock icon mask: rounded rectangle (not circle / squircle). */
val AppIconShape: Shape = RoundedCornerShape(14.dp)

/** @deprecated Prefer [AppIconShape]. Kept for any leftover refs. */
val IosSquircle: Shape = AppIconShape

val IosContinuousCorner = RoundedCornerShape(percent = 22)
