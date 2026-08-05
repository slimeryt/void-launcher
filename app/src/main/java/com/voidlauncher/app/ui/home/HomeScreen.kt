package com.voidlauncher.app.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.voidlauncher.app.SettingsActivity
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.DockBar
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.HomeClock
import com.voidlauncher.app.ui.components.IosSquircle
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenDrawerSearch: () -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onRemoveHomeItem: (page: Int, index: Int) -> Unit,
    onSwapHomeItems: (page: Int, a: Int, b: Int) -> Unit,
    onCreateFolder: (page: Int, target: Int, dragged: Int) -> Unit,
    onAddPage: () -> Unit,
    onAddAppToHome: (AppInfo, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageCount = state.pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    BackHandler(enabled = state.isEditMode) { onEditModeChange(false) }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragPage by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cellCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var swipeUp by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.isEditMode) {
                detectTapGestures(
                    onLongPress = { onEditModeChange(true) },
                    onTap = { if (state.isEditMode) onEditModeChange(false) }
                )
            }
            .pointerInput(state.isEditMode, state.isDrawerOpen) {
                if (state.isEditMode) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (swipeUp < -90f) onOpenDrawer()
                        swipeUp = 0f
                    },
                    onDragCancel = { swipeUp = 0f },
                    onVerticalDrag = { _, amount ->
                        if (amount < 0f) swipeUp += amount
                        else swipeUp = (swipeUp + amount).coerceAtMost(0f)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (state.isEditMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit", style = MaterialTheme.typography.titleMedium, color = VoidCyan)
                        IconButton(onClick = onAddPage) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add page", tint = VoidMist)
                        }
                    }
                }
            }

            if (!state.isEditMode) {
                Spacer(modifier = Modifier.height(40.dp))
                HomeClock()
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = dragIndex < 0
            ) { page ->
                val items = state.pages.getOrElse(page) { emptyList() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.gridColumns),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(items, key = { idx, item ->
                        when (item) {
                            is HomeItem.App -> "a:${item.key}:$idx"
                            is HomeItem.Folder -> "f:${item.id}:$idx"
                        }
                    }) { index, item ->
                        val isDragging = dragPage == page && dragIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    val c = coords.positionInWindow() + Offset(
                                        coords.size.width / 2f,
                                        coords.size.height / 2f
                                    )
                                    cellCenters = cellCenters + (index to c)
                                }
                                .pointerInput(state.isEditMode, page, index) {
                                    if (!state.isEditMode) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragPage = page
                                            dragIndex = index
                                            dragOffset = Offset.Zero
                                            onEditModeChange(true)
                                        },
                                        onDragEnd = {
                                            val from = dragIndex
                                            val dropPos = cellCenters[from]?.plus(dragOffset)
                                            if (from >= 0 && dropPos != null) {
                                                val target = cellCenters
                                                    .filterKeys { it != from }
                                                    .minByOrNull { (_, c) ->
                                                        hypot(
                                                            (c.x - dropPos.x).toDouble(),
                                                            (c.y - dropPos.y).toDouble()
                                                        )
                                                    }
                                                if (target != null) {
                                                    val dist = hypot(
                                                        (target.value.x - dropPos.x).toDouble(),
                                                        (target.value.y - dropPos.y).toDouble()
                                                    )
                                                    val threshold = with(density) { 56.dp.toPx() }
                                                    if (dist < threshold) {
                                                        val tItem = items.getOrNull(target.key)
                                                        val dItem = items.getOrNull(from)
                                                        if (tItem is HomeItem.App && dItem is HomeItem.App) {
                                                            onCreateFolder(page, target.key, from)
                                                        } else {
                                                            onSwapHomeItems(page, from, target.key)
                                                        }
                                                    }
                                                }
                                            }
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                        }
                                    )
                                }
                        ) {
                            when (item) {
                                is HomeItem.App -> {
                                    val app = state.appsByKey[item.key]
                                    if (app != null) {
                                        AppIcon(
                                            app = app,
                                            showLabel = state.showLabels,
                                            iconScale = state.iconScale,
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = {
                                                onEditModeChange(true)
                                                onAppLongClick(app)
                                            },
                                            editMode = state.isEditMode,
                                            onRemove = {
                                                onRemoveHomeItem(page, index)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isDragging) Modifier.offset {
                                                        IntOffset(
                                                            dragOffset.x.roundToInt(),
                                                            dragOffset.y.roundToInt()
                                                        )
                                                    } else Modifier
                                                )
                                        )
                                    }
                                }
                                is HomeItem.Folder -> {
                                    FolderIcon(
                                        name = state.folders[item.id]?.name ?: "Folder",
                                        apps = state.folders[item.id]?.appKeys
                                            ?.mapNotNull { state.appsByKey[it] }
                                            .orEmpty(),
                                        iconScale = state.iconScale,
                                        editMode = state.isEditMode,
                                        onClick = { /* folder open later */ },
                                        onLongClick = { onEditModeChange(true) },
                                        onRemove = { onRemoveHomeItem(page, index) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search pill (page 0) ↔ page dots (other pages / multi-page swipe)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val showDots = pageCount > 1 && pagerState.currentPage > 0
                AnimatedContent(
                    targetState = showDots,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pill-dots"
                ) { dots ->
                    if (dots) {
                        PageDots(
                            pageCount = pageCount,
                            current = pagerState.currentPage,
                            onDotClick = { scope.launch { pagerState.animateScrollToPage(it) } }
                        )
                    } else {
                        SearchPill(onClick = onOpenDrawerSearch)
                    }
                }
            }

            if (!state.isEditMode) {
                DockBar(
                    apps = state.dockApps,
                    iconScale = state.iconScale,
                    onAppClick = onLaunchApp,
                    onAppLongClick = { app ->
                        onEditModeChange(true)
                        onAppLongClick(app)
                    },
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassPanel(
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 28.dp,
                        strong = true,
                        enableSheen = false,
                        enableRefraction = false
                    ) {
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                                onEditModeChange(false)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = VoidMist,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap empty space to finish",
                        style = MaterialTheme.typography.labelMedium,
                        color = VoidMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier
            .width(56.dp)
            .height(24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 99.dp,
        strong = true,
        enableSheen = false,
        enableRefraction = true
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = VoidMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    current: Int,
    onDotClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) VoidMist else VoidMuted.copy(alpha = 0.45f))
                    .clickable { onDotClick(i) }
            )
        }
    }
}

@Composable
private fun FolderIcon(
    name: String,
    apps: List<AppInfo>,
    iconScale: Float,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val size = (56 * iconScale).dp
    val previews = remember(apps.map { it.key }) {
        apps.take(4).map { it.icon.toCachedBitmap(64).asImageBitmap() }
    }
    Column(
        modifier = modifier
            .width(80.dp)
            .padding(vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongClick() }, onTap = { onClick() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(IosSquircle)
                .background(Color(0x66FFFFFF))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
                            val cell = (size - 16.dp) / 2
                            val bmp = previews.getOrNull(idx)
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    filterQuality = FilterQuality.Low,
                                    modifier = Modifier
                                        .size(cell)
                                        .clip(IosSquircle)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(cell)
                                        .clip(IosSquircle)
                                        .background(Color(0x33FFFFFF))
                                )
                            }
                        }
                    }
                }
            }
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6FFFFFF))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.Black)
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = VoidMist
        )
    }
}
