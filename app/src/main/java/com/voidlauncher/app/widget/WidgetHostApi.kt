package com.voidlauncher.app.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.compositionLocalOf

/**
 * Bridges the home screen's Widgets UI (Compose) to the [AppWidgetHost] /
 * bind-and-configure flow, which must live in the hosting Activity.
 */
data class WidgetHostApi(
    val host: AppWidgetHost? = null,
    val manager: AppWidgetManager? = null,
    /** Bind [info] into a new host id, run configure if needed, then persist. */
    val onBindProvider: (AppWidgetProviderInfo) -> Unit = {},
    val onRemoveWidget: (Int) -> Unit = {}
)

val LocalWidgetHostApi = compositionLocalOf { WidgetHostApi() }
