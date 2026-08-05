package com.voidlauncher.app.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.SettingsActivity
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.AppIconShape
import com.voidlauncher.app.ui.components.DockBar
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.HomeClock
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.IosBlueGlass
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

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragPage by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cellCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var swipeUp by remember { mutableFloatStateOf(0f) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var removeZone by remember { mutableStateOf(Rect.Zero) }
    var overRemove by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = openFolderId != null) { openFolderId = null }
    BackHandler(enabled = state.isEditMode && openFolderId == null) { onEditModeChange(false) }

    LaunchedEffect(state.isEditMode) {
        if (!state.isEditMode) {
            selectedKeys = emptySet()
            dragIndex = -1
            dragPage = -1
            dragOffset = Offset.Zero
            overRemove = false
            openFolderId = null
        }
    }

    val editScale by animateFloatAsState(
        targetValue = if (state.isEditMode) 0.94f else 1f,
        animationSpec = tween(280),
        label = "edit-scale"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (state.isEditMode) 0.28f else 0f,
        animationSpec = tween(280),
        label = "edit-dim"
    )
    val isDragging = dragIndex >= 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.isEditMode) {
                if (state.isEditMode) return@pointerInput
                detectTapGestures(onLongPress = { onEditModeChange(true) })
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
        // Edit-mode dim overlay
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isEditMode) {
                    // Cancel / Done — hidden while dragging toward Remove
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isDragging,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        EditPillButton(
                            label = "Cancel",
                            onClick = { onEditModeChange(false) }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isDragging,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onAddPage) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add page",
                                    tint = VoidMist
                                )
                            }
                            EditPillButton(
                                label = "Done",
                                onClick = { onEditModeChange(false) },
                                blue = true
                            )
                        }
                    }

                    // Remove drop target — only while dragging
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDragging,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        RemoveDropPill(
                            highlighted = overRemove,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val pos = coords.positionInWindow()
                                removeZone = Rect(
                                    left = pos.x,
                                    top = pos.y,
                                    right = pos.x + coords.size.width,
                                    bottom = pos.y + coords.size.height
                                )
                            }
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = state.isEditMode,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)))
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "clock-edit"
            ) { editing ->
                if (!editing) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        HomeClock()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = editScale
                        scaleY = editScale
                    },
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
                        val dragging = dragPage == page && dragIndex == index
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
                                .pointerInput(state.isEditMode, page, index, item) {
                                    if (!state.isEditMode) return@pointerInput
                                    detectTapGestures(
                                        onTap = {
                                            when (item) {
                                                is HomeItem.App -> {
                                                    selectedKeys =
                                                        if (item.key in selectedKeys) selectedKeys - item.key
                                                        else selectedKeys + item.key
                                                }
                                                is HomeItem.Folder -> {
                                                    selectedKeys =
                                                        if (item.id in selectedKeys) selectedKeys - item.id
                                                        else selectedKeys + item.id
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(state.isEditMode, page, index) {
                                    if (!state.isEditMode) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragPage = page
                                            dragIndex = index
                                            dragOffset = Offset.Zero
                                            overRemove = false
                                        },
                                        onDragEnd = {
                                            val from = dragIndex
                                            val dropPos = cellCenters[from]?.plus(dragOffset)
                                            if (from >= 0 && dropPos != null) {
                                                if (removeZone.contains(dropPos)) {
                                                    onRemoveHomeItem(page, from)
                                                    val key = (items.getOrNull(from) as? HomeItem.App)?.key
                                                    if (key != null) {
                                                        selectedKeys = selectedKeys - key
                                                    }
                                                } else {
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
                                            }
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            overRemove = false
                                        },
                                        onDragCancel = {
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            overRemove = false
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            val pos = cellCenters[dragIndex]?.plus(dragOffset)
                                            overRemove = pos != null && removeZone.contains(pos)
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
                                            selected = item.key in selectedKeys,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(if (dragging && overRemove) 0.88f else 1f)
                                                .then(
                                                    if (dragging) Modifier.offset {
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
                                        selected = item.id in selectedKeys,
                                        onClick = {
                                            if (state.isEditMode) {
                                                selectedKeys =
                                                    if (item.id in selectedKeys) selectedKeys - item.id
                                                    else selectedKeys + item.id
                                            } else {
                                                openFolderId = item.id
                                            }
                                        },
                                        onLongClick = { onEditModeChange(true) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (dragging) Modifier.offset {
                                                    IntOffset(
                                                        dragOffset.x.roundToInt(),
                                                        dragOffset.y.roundToInt()
                                                    )
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!state.isEditMode) {
                    // Page 0 = search pill; other pages = dots. No offset hacks.
                    val currentPage = pagerState.currentPage
                    val showDots = pageCount > 1 && currentPage > 0
                    val dotsWidth = (24 + pageCount * 14).dp
                    val pillWidth = if (showDots) dotsWidth.coerceAtLeast(56.dp) else 56.dp
                    GlassPanel(
                        modifier = Modifier
                            .height(28.dp)
                            .width(pillWidth)
                            .clickable(
                                enabled = !showDots,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenDrawerSearch
                            ),
                        cornerRadius = 99.dp,
                        strong = true,
                        enableSheen = false,
                        enableRefraction = true
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDots) {
                                PageDots(
                                    pageCount = pageCount,
                                    current = currentPage,
                                    onDotClick = {
                                        scope.launch { pagerState.animateScrollToPage(it) }
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = VoidMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    contentAlignment = Alignment.Center
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
                }
            }
        }

        var displayedFolderId by remember { mutableStateOf<String?>(null) }
        if (openFolderId != null) displayedFolderId = openFolderId

        AnimatedVisibility(
            visible = openFolderId != null,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.82f, animationSpec = tween(280)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.88f, animationSpec = tween(220))
        ) {
            val folderId = displayedFolderId
            val folder = folderId?.let { state.folders[it] }
            if (folder != null) {
                OpenFolderOverlay(
                    name = folder.name,
                    apps = folder.appKeys.mapNotNull { state.appsByKey[it] },
                    iconScale = state.iconScale,
                    onDismiss = { openFolderId = null },
                    onLaunchApp = onLaunchApp
                )
            }
        }
    }
}

@Composable
private fun RemoveDropPill(
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (highlighted) Color(0xFFE5484D) else Color(0xE61C1F26))
            .border(
                width = 1.dp,
                color = if (highlighted) Color.White.copy(alpha = 0.5f) else Color(0x44FFFFFF),
                shape = RoundedCornerShape(99.dp)
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Remove",
            style = MaterialTheme.typography.labelLarge,
            color = VoidMist
        )
    }
}

@Composable
private fun EditPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    blue: Boolean = false
) {
    GlassPanel(
        modifier = modifier
            .height(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 99.dp,
        strong = true,
        enableSheen = false,
        enableRefraction = !blue,
        tint = if (blue) IosBlueGlass else Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = VoidMist
            )
        }
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    current: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.wrapContentWidth()
    ) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
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
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val size = (56 * iconScale).dp
    val previews = remember(apps.map { it.key }) {
        apps.take(9).map { it.icon.toCachedBitmap(64, cornerRadiusRatio = 0.24f).asImageBitmap() }
    }
    val pad = 5.dp
    val gap = 2.dp
    val cell = (size - pad * 2 - gap * 2) / 3
    Column(
        modifier = modifier
            .width(80.dp)
            .padding(vertical = 6.dp)
            .then(
                if (editMode) Modifier
                else Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongClick() }, onTap = { onClick() })
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Unclipped outer box so the check sits outside the folder clip (like AppIcon)
        Box {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AppIconShape)
                    .background(Color(0x66FFFFFF))
                    .padding(pad)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0 until 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (col in 0 until 3) {
                                val idx = row * 3 + col
                                val bmp = previews.getOrNull(idx)
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        filterQuality = FilterQuality.Low,
                                        modifier = Modifier
                                            .size(cell)
                                            .clip(AppIconShape)
                                    )
                                } else {
                                    // Empty slot — no placeholder wrapper
                                    Spacer(modifier = Modifier.size(cell))
                                }
                            }
                        }
                    }
                }
            }
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) IosBlue else Color(0xE6FFFFFF))
                        .border(
                            width = 1.5.dp,
                            color = if (selected) IosBlue else Color(0x66FFFFFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = VoidMist,
            maxLines = 1
        )
    }
}

@Composable
private fun OpenFolderOverlay(
    name: String,
    apps: List<AppInfo>,
    iconScale: Float,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit
) {
    // Fixed shell — same size regardless of app count
    val panelWidth = 312.dp
    val titleBlock = 44.dp
    val gridHeight = 268.dp
    val panelHeight = titleBlock + gridHeight + 36.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier
                .width(panelWidth)
                .height(panelHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            cornerRadius = 28.dp,
            strong = true,
            enableSheen = false,
            enableRefraction = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = VoidMist,
                    modifier = Modifier.height(titleBlock),
                    maxLines = 1
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    items(apps.size) { i ->
                        val app = apps[i]
                        AppIcon(
                            app = app,
                            iconScale = iconScale * 0.92f,
                            showLabel = true,
                            onClick = {
                                onDismiss()
                                onLaunchApp(app)
                            },
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
