package com.voidlauncher.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

/** Percent-based so tiny folder thumbs keep the same proportions as full icons. */
val AppIconShape: Shape = RoundedCornerShape(percent = 24)

/** @deprecated Prefer [AppIconShape]. Kept for any leftover refs. */
val IosSquircle: Shape = AppIconShape

val IosContinuousCorner = RoundedCornerShape(percent = 24)
