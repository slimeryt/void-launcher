package com.voidlauncher.app.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.SettingsActivity
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.AppIconShape
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.DockBar
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.HomeClock
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.ui.pickers.WallpaperPickerOverlay
import com.voidlauncher.app.ui.pickers.WidgetPickerOverlay
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.IosBlueGlass
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState
import com.voidlauncher.app.widget.LocalWidgetHostApi
import com.voidlauncher.app.widget.WidgetView
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
    onAddAppToFolder: (page: Int, folderIndex: Int, appIndex: Int) -> Unit,
    onAddPage: () -> Unit,
    onAddAppToHome: (AppInfo, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageCount = state.pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val view = androidx.compose.ui.platform.LocalView.current

    // 0..1 progress across the *whole* wallpaper width, matching what we tell the
    // system via setWallpaperOffsets, so our own glass sampling and the real
    // system-drawn wallpaper behind us always agree on which slice is visible.
    val wallpaperXOffset by remember(pageCount) {
        androidx.compose.runtime.derivedStateOf {
            if (pageCount <= 1) {
                0.5f
            } else {
                ((pagerState.currentPage + pagerState.currentPageOffsetFraction) /
                    (pageCount - 1).toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    LaunchedEffect(wallpaperXOffset) {
        runCatching {
            android.app.WallpaperManager.getInstance(context)
                .setWallpaperOffsets(view.windowToken, wallpaperXOffset, 0.5f)
        }
    }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragPage by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cellCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var swipeUp by remember { mutableFloatStateOf(0f) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var removeZone by remember { mutableStateOf(Rect.Zero) }
    var overRemove by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var openFolderSource by remember { mutableStateOf(Rect.Zero) }
    var folderClosing by remember { mutableStateOf(false) }
    var folderBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = showWidgetPicker || showWallpaperPicker) {
        showWidgetPicker = false
        showWallpaperPicker = false
    }
    BackHandler(enabled = openFolderId != null) {
        if (!folderClosing) folderClosing = true
    }
    BackHandler(enabled = state.isEditMode && openFolderId == null) { onEditModeChange(false) }

    LaunchedEffect(state.isEditMode) {
        if (!state.isEditMode) {
            selectedKeys = emptySet()
            dragIndex = -1
            dragPage = -1
            dragOffset = Offset.Zero
            overRemove = false
            openFolderId = null
            folderClosing = false
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

    androidx.compose.runtime.CompositionLocalProvider(
        com.voidlauncher.app.glass.LocalWallpaperXOffset provides wallpaperXOffset
    ) {
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

            val widgetApi = LocalWidgetHostApi.current
            if (state.widgetIds.isNotEmpty()) {
                val host = widgetApi.host
                val manager = widgetApi.manager
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer {
                            scaleX = editScale
                            scaleY = editScale
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.widgetIds.forEach { widgetId ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (host != null && manager != null) {
                                GlassPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 22.dp,
                                    enableSheen = false,
                                    enableRefraction = false
                                ) {
                                    WidgetView(host = host, manager = manager, widgetId = widgetId)
                                }
                            }
                            if (state.isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xE6FFFFFF))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { widgetApi.onRemoveWidget(widgetId) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove widget",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
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
                                                            when {
                                                                tItem is HomeItem.App && dItem is HomeItem.App ->
                                                                    onCreateFolder(page, target.key, from)
                                                                tItem is HomeItem.Folder && dItem is HomeItem.App ->
                                                                    onAddAppToFolder(page, target.key, from)
                                                                tItem is HomeItem.App && dItem is HomeItem.Folder ->
                                                                    onAddAppToFolder(page, from, target.key)
                                                                else ->
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
                                            onLongClick = { onAppLongClick(app) },
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
                                                openFolderSource = folderBounds[item.id] ?: Rect.Zero
                                                folderClosing = false
                                                openFolderId = item.id
                                            }
                                        },
                                        onLongClick = { onEditModeChange(true) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                val pos = coords.positionInWindow()
                                                val size = coords.size
                                                folderBounds = folderBounds + (item.id to Rect(
                                                    pos.x,
                                                    pos.y,
                                                    pos.x + size.width,
                                                    pos.y + size.height
                                                ))
                                            }
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
                        enableSheen = true,
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
                    showLabels = state.dockLabels,
                    onAppClick = onLaunchApp,
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditFooterButton(
                        icon = Icons.Rounded.Widgets,
                        contentDescription = "Widgets",
                        onClick = { showWidgetPicker = true }
                    )
                    EditFooterButton(
                        icon = Icons.Rounded.Wallpaper,
                        contentDescription = "Wallpaper",
                        onClick = { showWallpaperPicker = true }
                    )
                    EditFooterButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }

        openFolderId?.let { folderId ->
            val folder = state.folders[folderId]
            if (folder != null) {
                OpenFolderOverlay(
                    name = folder.name,
                    apps = folder.appKeys.mapNotNull { state.appsByKey[it] },
                    iconScale = state.iconScale,
                    sourceBounds = openFolderSource,
                    closing = folderClosing,
                    onRequestClose = { folderClosing = true },
                    onCloseFinished = {
                        openFolderId = null
                        folderClosing = false
                    },
                    onLaunchApp = onLaunchApp
                )
            }
        }

        val widgetApi = LocalWidgetHostApi.current
        WidgetPickerOverlay(
            visible = showWidgetPicker,
            onDismiss = { showWidgetPicker = false },
            onPick = { info -> widgetApi.onBindProvider(info) }
        )
        WallpaperPickerOverlay(
            visible = showWallpaperPicker,
            onDismiss = { showWallpaperPicker = false }
        )
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
            .clip(CapsuleShape)
            .background(if (highlighted) Color(0xFFE5484D) else Color(0xE61C1F26))
            .border(
                width = 1.dp,
                color = if (highlighted) Color.White.copy(alpha = 0.5f) else Color(0x44FFFFFF),
                shape = CapsuleShape
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
private fun EditFooterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.size(56.dp),
        cornerRadius = 28.dp,
        strong = true,
        enableSheen = false,
        enableRefraction = false
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = VoidMist,
                modifier = Modifier.size(26.dp)
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
    val appearance = LocalIconAppearance.current
    val effectiveScale = appearance.scale
    val size = (56 * effectiveScale).dp
    val radiusRatio = appearance.cornerRadiusRatio
    val shape = remember(appearance.shapePercent) {
        SmoothCornerShape(percent = appearance.shapePercent.coerceAtLeast(1))
    }
    val colorFilter = remember(appearance.theme, appearance.tintHue, appearance.tintAlpha) {
        appearance.colorFilter()
    }
    val previews = remember(apps.map { it.key }, radiusRatio) {
        apps.take(9).map { it.icon.toCachedBitmap(64, cornerRadiusRatio = radiusRatio).asImageBitmap() }
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
                    .clip(shape)
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
                                        colorFilter = colorFilter,
                                        modifier = Modifier
                                            .size(cell)
                                            .clip(shape)
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
    sourceBounds: Rect,
    closing: Boolean,
    onRequestClose: () -> Unit,
    onCloseFinished: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit
) {
    val panelWidth = 312.dp
    val titleBlock = 44.dp
    val gridHeight = 268.dp
    val panelHeight = titleBlock + gridHeight + 36.dp
    val density = LocalDensity.current

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(closing) {
        if (!closing) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        } else {
            progress.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            onCloseFinished()
        }
    }

    val t = progress.value
    val panelWpx = with(density) { panelWidth.toPx() }
    val panelHpx = with(density) { panelHeight.toPx() }
    val destCx = rootPos.x + rootSize.width / 2f
    val destCy = rootPos.y + rootSize.height / 2f
    val srcCx = if (sourceBounds.width > 1f) sourceBounds.center.x else destCx
    val srcCy = if (sourceBounds.height > 1f) sourceBounds.center.y else destCy
    val srcScale = if (sourceBounds.width > 1f && panelWpx > 1f) {
        (sourceBounds.width / panelWpx).coerceIn(0.12f, 0.95f)
    } else {
        0.35f
    }
    val scale = srcScale + (1f - srcScale) * t
    val translateX = (srcCx - destCx) * (1f - t)
    val translateY = (srcCy - destCy) * (1f - t)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootSize = coords.size
                rootPos = coords.positionInWindow()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * t))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRequestClose
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassPanel(
                modifier = Modifier
                    .width(panelWidth)
                    .height(panelHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = translateX
                        translationY = translateY
                        alpha = (0.35f + 0.65f * t).coerceIn(0f, 1f)
                    }
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
                                    onRequestClose()
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
}
