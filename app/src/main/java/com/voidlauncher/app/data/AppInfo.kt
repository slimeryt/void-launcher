package com.voidlauncher.app.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false
) {
    val key: String get() = "$packageName/$activityName"
}
