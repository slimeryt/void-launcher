package com.voidlauncher.app.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.runtime.compositionLocalOf

/**
 * Bridges the home screen's "Widgets" button (Compose) to the [AppWidgetHost] /
 * pick-and-bind flow, which must live in the hosting Activity.
 */
data class WidgetHostApi(
    val host: AppWidgetHost? = null,
    val manager: AppWidgetManager? = null,
    val onAddWidget: () -> Unit = {},
    val onRemoveWidget: (Int) -> Unit = {}
)

val LocalWidgetHostApi = compositionLocalOf { WidgetHostApi() }
