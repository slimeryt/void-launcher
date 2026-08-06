package com.voidlauncher.app.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts a live [AppWidgetHostView] for [widgetId] inside Compose, sized to its declared minimum. */
@Composable
fun WidgetView(
    host: AppWidgetHost,
    manager: AppWidgetManager,
    widgetId: Int,
    modifier: Modifier = Modifier
) {
    val info = remember(widgetId) { manager.getAppWidgetInfo(widgetId) } ?: return
    val density = LocalDensity.current
    val heightDp = with(density) { info.minHeight.toFloat().toDp() }.let { if (it.value < 60f) 60.dp else it }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp),
        factory = { ctx ->
            (host.createView(ctx, widgetId, info) as AppWidgetHostView).apply {
                setAppWidget(widgetId, info)
            }
        },
        update = { view -> view.setAppWidget(widgetId, info) }
    )
}
