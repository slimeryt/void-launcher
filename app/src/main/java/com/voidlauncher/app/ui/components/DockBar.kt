package com.voidlauncher.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.gestures.detectLongPressMenuOrDrag
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    apps: List<AppInfo>,
    iconScale: Float,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onAppMenuDismiss: () -> Unit,
    onAppMenuArmDismiss: () -> Unit = {},
    onSwapDockItems: (a: Int, b: Int) -> Unit,
    onMoveDockAppToHome: (AppInfo) -> Unit = {},
    onDockDragChanged: ((AppInfo?, Offset?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false
) {
    val density = LocalDensity.current
    var dragIndex by remember { mutableIntStateOf(-1) }
    var hoverIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragAnchor by remember { mutableStateOf(Offset.Zero) }
    var slotCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var cellBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }

    val displayApps = remember(apps, dragIndex, hoverIndex) {
        if (dragIndex < 0 || hoverIndex < 0 || dragIndex == hoverIndex) apps
        else {
            val next = apps.toMutableList()
            val app = next.removeAt(dragIndex)
            next.add(hoverIndex.coerceIn(0, next.size), app)
            next
        }
    }

    fun reportDrag(finger: Offset) {
        val app = apps.getOrNull(dragIndex) ?: return
        onDockDragChanged?.invoke(app, finger)
    }

    fun clearDragReport() {
        onDockDragChanged?.invoke(null, null)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootPos = it.positionInWindow() }
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    dockBounds = Rect(
                        pos.x,
                        pos.y,
                        pos.x + coords.size.width.toFloat(),
                        pos.y + coords.size.height.toFloat()
                    )
                },
            cornerRadius = 32.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = false
            ) {
                itemsIndexed(
                    items = displayApps,
                    key = { _, app -> app.key }
                ) { displayIndex, app ->
                    val sourceIndex = apps.indexOfFirst { it.key == app.key }.takeIf { it >= 0 }
                        ?: displayIndex
                    val isGhost = dragIndex >= 0 && sourceIndex == dragIndex
                    Box(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                val w = coords.size.width.toFloat()
                                val h = coords.size.height.toFloat()
                                cellBounds = cellBounds + (displayIndex to Rect(pos.x, pos.y, pos.x + w, pos.y + h))
                                slotCenters = slotCenters + (displayIndex to Offset(pos.x + w / 2f, pos.y + h / 2f))
                            }
                            .pointerInput(sourceIndex, apps.size) {
                                detectLongPressMenuOrDrag(
                                    onLongPress = {
                                        val src = apps.getOrNull(sourceIndex)
                                            ?: return@detectLongPressMenuOrDrag
                                        onAppLongClick(src, cellBounds[displayIndex] ?: Rect.Zero)
                                    },
                                    onLongPressRelease = { onAppMenuArmDismiss() },
                                    onDragStart = {
                                        onAppMenuDismiss()
                                        dragIndex = sourceIndex
                                        hoverIndex = sourceIndex
                                        dragOffset = Offset.Zero
                                        dragAnchor = slotCenters[displayIndex] ?: Offset.Zero
                                        reportDrag(dragAnchor)
                                    },
                                    onDragEnd = {
                                        val from = dragIndex
                                        val dropPos = dragAnchor + dragOffset
                                        if (from >= 0) {
                                            val outsideDock = dockBounds.width > 1f &&
                                                !dockBounds.expandBy(12f).contains(dropPos)
                                            if (outsideDock) {
                                                apps.getOrNull(from)?.let(onMoveDockAppToHome)
                                            } else if (hoverIndex >= 0 && hoverIndex != from) {
                                                var i = from
                                                val target = hoverIndex
                                                if (i < target) {
                                                    while (i < target) {
                                                        onSwapDockItems(i, i + 1)
                                                        i++
                                                    }
                                                } else {
                                                    while (i > target) {
                                                        onSwapDockItems(i, i - 1)
                                                        i--
                                                    }
                                                }
                                            }
                                        }
                                        dragIndex = -1
                                        hoverIndex = -1
                                        dragOffset = Offset.Zero
                                        clearDragReport()
                                    },
                                    onDragCancel = {
                                        dragIndex = -1
                                        hoverIndex = -1
                                        dragOffset = Offset.Zero
                                        clearDragReport()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        val finger = dragAnchor + dragOffset
                                        reportDrag(finger)
                                        hoverIndex = slotCenters.minByOrNull { (_, c) ->
                                            hypot(
                                                (c.x - finger.x).toDouble(),
                                                (c.y - finger.y).toDouble()
                                            )
                                        }?.key ?: hoverIndex
                                    }
                                )
                            }
                            .alpha(if (isGhost) 0f else 1f)
                    ) {
                        AppIcon(
                            app = app,
                            showLabel = showLabels,
                            iconScale = iconScale,
                            onClick = { onAppClick(app) },
                            onLongClick = {},
                            longPressEnabled = false
                        )
                    }
                }
            }
        }

        if (onDockDragChanged == null) {
            val floating = apps.getOrNull(dragIndex)
            if (floating != null && dragIndex >= 0) {
                val halfW = with(density) { 40.dp.toPx() }
                val halfH = with(density) { 48.dp.toPx() }
                val finger = dragAnchor + dragOffset
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(20f)
                        .offset {
                            IntOffset(
                                (finger.x - rootPos.x - halfW).roundToInt(),
                                (finger.y - rootPos.y - halfH).roundToInt()
                            )
                        }
                ) {
                    AppIcon(
                        app = floating,
                        showLabel = showLabels,
                        iconScale = iconScale,
                        onClick = {},
                        onLongClick = {},
                        longPressEnabled = false
                    )
                }
            }
        }
    }
}

private fun Rect.expandBy(amount: Float): Rect = Rect(
    left - amount,
    top - amount,
    right + amount,
    bottom + amount
)
