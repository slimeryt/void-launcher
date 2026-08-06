package com.voidlauncher.app.ui.pickers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted

private data class WidgetAppGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)

/** Classic launcher cell conversion: `(dp + 30) / 70`. */
private fun cellsForSizeDp(sizeDp: Float): Int {
    var n = 2
    while (70 * n - 30 < sizeDp) n++
    return (n - 1).coerceAtLeast(1)
}

private fun AppWidgetProviderInfo.spanCells(density: Float): Pair<Int, Int> {
    val cols = if (Build.VERSION.SDK_INT >= 31 && targetCellWidth > 0) {
        targetCellWidth
    } else {
        cellsForSizeDp(minWidth / density)
    }
    val rows = if (Build.VERSION.SDK_INT >= 31 && targetCellHeight > 0) {
        targetCellHeight
    } else {
        cellsForSizeDp(minHeight / density)
    }
    return cols.coerceAtLeast(1) to rows.coerceAtLeast(1)
}

@Composable
fun WidgetPickerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val groups = remember {
        val pm = context.packageManager
        val manager = AppWidgetManager.getInstance(context)
        manager.installedProviders
            .groupBy { it.provider.packageName }
            .map { (pkg, widgets) ->
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                WidgetAppGroup(
                    packageName = pkg,
                    appLabel = appInfo?.loadLabel(pm)?.toString() ?: pkg,
                    appIcon = appInfo?.loadIcon(pm),
                    widgets = widgets.sortedBy { it.loadLabel(pm).toString() }
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        WidgetPickerBody(
            groups = groups,
            densityDpi = densityDpi,
            onDismiss = onDismiss,
            onPick = onPick
        )
    }
}

@Composable
private fun WidgetPickerBody(
    groups: List<WidgetAppGroup>,
    densityDpi: Int,
    onDismiss: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.titleLarge,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = VoidMist)
            }
        }

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No widgets found", color = VoidMuted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groups, key = { it.packageName }) { group ->
                    WidgetAppSection(
                        group = group,
                        densityDpi = densityDpi,
                        onPick = {
                            onPick(it)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetAppSection(
    group: WidgetAppGroup,
    densityDpi: Int,
    onPick: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appIcon = group.appIcon
    val appBmp = remember(group.packageName, appIcon) {
        appIcon?.toCachedBitmap(maxSize = 96, cornerRadiusRatio = 0.22f)?.asImageBitmap()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (appBmp != null) {
                Image(
                    bitmap = appBmp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(SmoothCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = group.appLabel,
                style = MaterialTheme.typography.titleMedium,
                color = VoidMist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        group.widgets.forEach { info ->
            WidgetPreviewCard(
                info = info,
                label = info.loadLabel(pm).toString(),
                densityDpi = densityDpi,
                onClick = { onPick(info) }
            )
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    info: AppWidgetProviderInfo,
    label: String,
    densityDpi: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val (cols, rows) = remember(info.provider) { info.spanCells(density) }
    val span = "${cols}×${rows}"

    val previewBmp = remember(info.provider, densityDpi) {
        runCatching {
            val drawable = info.loadPreviewImage(context, densityDpi)
                ?: info.loadIcon(context, densityDpi)
            // Keep corners from the preview asset itself — don't wrap / re-round heavily.
            drawable?.toCachedBitmap(maxSize = 1024, cornerRadiusRatio = 0f)?.asImageBitmap()
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val aspect = (cols.toFloat() / rows.toFloat()).coerceIn(0.45f, 3.2f)
        if (previewBmp != null) {
            Image(
                bitmap = previewBmp,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect),
                contentAlignment = Alignment.Center
            ) {
                Text("No preview", color = VoidMuted)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = VoidMist,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = span,
                color = VoidMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
