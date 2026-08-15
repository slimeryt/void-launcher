package com.voidlauncher.app.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.voidlauncher.app.ui.statusbar.polarStatusPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.voidlauncher.app.SettingsActivity
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.ui.assistant.AssistantOverlay
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.gestures.detectLongPressMenuOrDrag
import com.voidlauncher.app.ui.gestures.detectUnconsumedLongPress
import com.voidlauncher.app.ui.shade.ccRubberBand
import com.voidlauncher.app.util.PendingLaunchBounds
import com.voidlauncher.app.ui.components.AppIconShape
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.DockBar
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.glass.LocalWallpaperScrollState
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import com.voidlauncher.app.glass.WallpaperScrollState
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.components.WallpaperHazeSource
import com.voidlauncher.app.ui.components.liquidGlassStroke
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.ui.pickers.PageManagerOverlay
import com.voidlauncher.app.ui.pickers.WallpaperPickerOverlay
import com.voidlauncher.app.ui.pickers.WidgetPickerOverlay
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.IosBlueGlass
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState
import com.voidlauncher.app.widget.LocalWidgetHostApi
import com.voidlauncher.app.widget.WidgetView
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onAppMenuDismiss: () -> Unit,
    onAppMenuArmDismiss: () -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenDrawerSearch: () -> Unit,
    onOpenNotificationCenter: () -> Unit = {},
    onOpenControlCenter: () -> Unit = {},
    onControlCenterPull: (Float) -> Unit = {},
    onControlCenterPullEnd: (Boolean) -> Unit = {},
    onSearchApps: (query: String) -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onRemoveHomeItem: (page: Int, index: Int) -> Unit,
    onSwapHomeItems: (page: Int, a: Int, b: Int) -> Unit,
    onMoveHomeItem: (fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int) -> Unit,
    onSwapDockItems: (a: Int, b: Int) -> Unit,
    onMoveDockAppToHome: (AppInfo, page: Int) -> Unit = { _, _ -> },
    onCreateFolder: (page: Int, target: Int, dragged: Int) -> Unit,
    onAddAppToFolder: (page: Int, folderIndex: Int, appIndex: Int) -> Unit,
    onExtractAppFromFolder: (folderId: String, appKey: String, pageIndex: Int) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: (pageIndex: Int) -> Unit,
    onAddAppToHome: (AppInfo, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pageCount = state.pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val view = androidx.compose.ui.platform.LocalView.current
    var assistantOpen by remember { mutableStateOf(false) }

    val wallpaperScroll = remember { WallpaperScrollState() }
    // Wallpaper stays fixed while pages swipe — glass sampling + system wallpaper
    // both use the centered slice (0.5) so they stay in sync without parallax.
    val wallpaperXOffset = 0.5f
    wallpaperScroll.offset = wallpaperXOffset
    androidx.compose.runtime.SideEffect {
        runCatching {
            android.app.WallpaperManager.getInstance(context)
                .setWallpaperOffsets(view.windowToken, wallpaperXOffset, 0.5f)
        }
    }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragPage by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragAnchor by remember { mutableStateOf(Offset.Zero) }
    var hoverIndex by remember { mutableIntStateOf(-1) }
    var cellCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var cellBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var folderPreviewTarget by remember { mutableIntStateOf(-1) }
    var dragFinger by remember { mutableStateOf(Offset.Zero) }
    var homeRootPos by remember { mutableStateOf(Offset.Zero) }
    var swipeUp by remember { mutableFloatStateOf(0f) }
    var swipeDown by remember { mutableFloatStateOf(0f) }
    var swipeStart by remember { mutableStateOf(Offset.Zero) }
    var pullingCc by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var removeZone by remember { mutableStateOf(Rect.Zero) }
    var overRemove by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var openFolderSource by remember { mutableStateOf(Rect.Zero) }
    var folderClosing by remember { mutableStateOf(false) }
    var folderBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var showPageManager by remember { mutableStateOf(false) }
    var dockDragApp by remember { mutableStateOf<AppInfo?>(null) }
    var dockDragFinger by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(openFolderId, state.folders) {
        val id = openFolderId ?: return@LaunchedEffect
        if (id !in state.folders) {
            openFolderId = null
            folderClosing = false
        }
    }

    BackHandler(enabled = showWidgetPicker || showWallpaperPicker || showPageManager) {
        showWidgetPicker = false
        showWallpaperPicker = false
        showPageManager = false
    }
    BackHandler(enabled = openFolderId != null) {
        if (!folderClosing) folderClosing = true
    }
    BackHandler(enabled = assistantOpen) { assistantOpen = false }
    BackHandler(enabled = state.isEditMode && openFolderId == null && !assistantOpen) {
        onEditModeChange(false)
    }

    LaunchedEffect(state.isEditMode) {
        if (!state.isEditMode) {
            selectedKeys = emptySet()
            dragIndex = -1
            dragPage = -1
            dragOffset = Offset.Zero
            hoverIndex = -1
            overRemove = false
            folderPreviewTarget = -1
            openFolderId = null
            folderClosing = false
        }
    }

    val editScale by animateFloatAsState(
        targetValue = if (state.isEditMode) 0.92f else 1f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "edit-scale"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (state.isEditMode) 0.28f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "edit-dim"
    )
    val isDragging = dragIndex >= 0
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // Hold at page edges while dragging → flip pages; dragged app stays on finger
    LaunchedEffect(isDragging, pageCount) {
        if (!isDragging || pageCount <= 1) return@LaunchedEffect
        val edgePx = with(density) { 40.dp.toPx() }
        while (dragIndex >= 0) {
            val x = dragFinger.x
            val dir = when {
                x < edgePx && pagerState.currentPage > 0 -> -1
                x > screenWidthPx - edgePx && pagerState.currentPage < pageCount - 1 -> 1
                else -> 0
            }
            if (dir == 0) {
                delay(40)
                continue
            }
            delay(420)
            if (dragIndex < 0) break
            val x2 = dragFinger.x
            val still = when (dir) {
                -1 -> x2 < edgePx && pagerState.currentPage > 0
                else -> x2 > screenWidthPx - edgePx && pagerState.currentPage < pageCount - 1
            }
            if (still) {
                val target = (pagerState.currentPage + dir).coerceIn(0, pageCount - 1)
                pagerState.animateScrollToPage(target)
                delay(380)
            }
        }
    }

    CompositionLocalProvider(
        LocalWallpaperScrollState provides wallpaperScroll,
        LocalWallpaperXOffset provides wallpaperXOffset
    ) {
    val hazeState = LocalHazeState.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { homeRootPos = it.positionInWindow() }
            .pointerInput(state.isEditMode, state.isDrawerOpen, state.isControlCenterOpen) {
                if (state.isEditMode) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        swipeStart = offset
                        swipeUp = 0f
                        swipeDown = 0f
                        pullingCc = !state.isControlCenterOpen &&
                            offset.y < size.height * 0.38f &&
                            offset.x >= size.width * 0.5f
                    },
                    onDragEnd = {
                        if (pullingCc) {
                            val revealPx = size.height * 0.38f
                            onControlCenterPullEnd(swipeDown > revealPx * 0.32f)
                            pullingCc = false
                            swipeUp = 0f
                            swipeDown = 0f
                            return@detectVerticalDragGestures
                        }
                        // Swipe down from top band: left half → NC, right half → CC
                        val fromTop = swipeStart.y < size.height * 0.38f
                        when {
                            fromTop && swipeDown > 80f -> {
                                if (swipeStart.x < size.width * 0.5f) onOpenNotificationCenter()
                                else onOpenControlCenter()
                            }
                            swipeUp < -90f -> onOpenDrawer()
                        }
                        swipeUp = 0f
                        swipeDown = 0f
                    },
                    onDragCancel = {
                        if (pullingCc) {
                            onControlCenterPullEnd(false)
                            pullingCc = false
                        }
                        swipeUp = 0f
                        swipeDown = 0f
                    },
                    onVerticalDrag = { _, amount ->
                        if (pullingCc) {
                            swipeDown = (swipeDown + amount).coerceAtLeast(0f)
                            val revealPx = size.height * 0.38f
                            val progress = if (swipeDown <= revealPx) {
                                swipeDown / revealPx
                            } else {
                                1f + ccRubberBand(swipeDown - revealPx, 220f) / revealPx
                            }
                            onControlCenterPull(progress)
                            return@detectVerticalDragGestures
                        }
                        when {
                            amount > 0f -> {
                                swipeDown += amount
                                swipeUp = (swipeUp + amount).coerceAtMost(0f)
                            }
                            amount < 0f -> {
                                swipeUp += amount
                                swipeDown = (swipeDown + amount).coerceAtLeast(0f)
                            }
                        }
                    }
                )
            }
    ) {
        if (hazeState != null) {
            WallpaperHazeSource(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState, zIndex = 0f)
            )
        }

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
                .polarStatusPadding()
                .navigationBarsPadding()
                .pointerInput(state.isEditMode, state.isDrawerOpen) {
                    if (state.isEditMode || state.isDrawerOpen) return@pointerInput
                    detectUnconsumedLongPress { onEditModeChange(true) }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp)
                    .pointerInput(state.isEditMode) {
                        if (state.isEditMode) return@pointerInput
                        detectTapGestures(onLongPress = { onEditModeChange(true) })
                    },
                contentAlignment = Alignment.Center
            ) {
                val editChromeVisible = state.isEditMode && !isDragging
                val densityChrome = LocalDensity.current
                // From top-left / top-right corners into place (layout offset = glass tracks).
                val cancelFrom = remember(densityChrome) {
                    with(densityChrome) { IntOffset((-110).dp.roundToPx(), (-90).dp.roundToPx()) }
                }
                val doneFrom = remember(densityChrome) {
                    with(densityChrome) { IntOffset(110.dp.roundToPx(), (-90).dp.roundToPx()) }
                }
                GlassAwareSlide(
                    visible = editChromeVisible,
                    enterFrom = cancelFrom,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    EditPillButton(
                        label = "Cancel",
                        onClick = { onEditModeChange(false) }
                    )
                }
                GlassAwareSlide(
                    visible = editChromeVisible,
                    enterFrom = doneFrom,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showPageManager = true }) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Manage pages",
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

                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isEditMode && isDragging,
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
                            transformOrigin = TransformOrigin.Center
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
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeSource(state = hazeState, zIndex = 1f)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        scaleX = editScale
                        scaleY = editScale
                        transformOrigin = TransformOrigin.Center
                    },
                userScrollEnabled = dragIndex < 0
            ) { page ->
                val rawItems = state.pages.getOrElse(page) { emptyList() }
                val draggingOnPage = dragPage == page && dragIndex >= 0
                val displayItems = if (
                    draggingOnPage &&
                    hoverIndex >= 0 &&
                    folderPreviewTarget < 0
                ) {
                    reorderPreview(rawItems, dragIndex, hoverIndex)
                } else {
                    rawItems
                }
                val draggedItem = if (draggingOnPage) rawItems.getOrNull(dragIndex) else null
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.gridColumns),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(displayItems, key = { _, item ->
                        when (item) {
                            is HomeItem.App -> "a:${item.key}"
                            is HomeItem.Folder -> "f:${item.id}"
                        }
                    }) { index, item ->
                        val isDraggedSlot = draggedItem != null && sameHomeItem(draggedItem, item)
                        // Original index in raw list (for gestures / drop commit)
                        val sourceIndex = when (item) {
                            is HomeItem.App -> rawItems.indexOfFirst {
                                it is HomeItem.App && it.key == item.key
                            }
                            is HomeItem.Folder -> rawItems.indexOfFirst {
                                it is HomeItem.Folder && it.id == item.id
                            }
                        }.takeIf { it >= 0 } ?: index
                        val dragging = isDraggedSlot
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = 0.86f
                                    )
                                )
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInWindow()
                                    val w = coords.size.width.toFloat()
                                    val h = coords.size.height.toFloat()
                                    // Slot centers follow the *display* grid under the finger.
                                    cellBounds = cellBounds + (index to Rect(pos.x, pos.y, pos.x + w, pos.y + h))
                                    cellCenters = cellCenters + (index to Offset(pos.x + w / 2f, pos.y + h / 2f))
                                }
                                .pointerInput(state.isEditMode, page, sourceIndex, item) {
                                    if (state.isEditMode) return@pointerInput
                                    detectLongPressMenuOrDrag(
                                        onTap = {
                                            when (item) {
                                                is HomeItem.App -> {
                                                    val app = state.appsByKey[item.key]
                                                    if (app != null) {
                                                        cellBounds[index]?.let { r ->
                                                            PendingLaunchBounds.rect =
                                                                android.graphics.Rect(
                                                                    r.left.toInt(),
                                                                    r.top.toInt(),
                                                                    r.right.toInt(),
                                                                    r.bottom.toInt()
                                                                )
                                                        }
                                                        onLaunchApp(app)
                                                    }
                                                }
                                                is HomeItem.Folder -> {
                                                    openFolderSource =
                                                        folderBounds[item.id] ?: Rect.Zero
                                                    folderClosing = false
                                                    openFolderId = item.id
                                                }
                                            }
                                        },
                                        onLongPress = {
                                            when (item) {
                                                is HomeItem.App -> {
                                                    val app = state.appsByKey[item.key]
                                                    if (app != null) {
                                                        onAppLongClick(
                                                            app,
                                                            cellBounds[index] ?: Rect.Zero
                                                        )
                                                    }
                                                }
                                                is HomeItem.Folder -> Unit
                                            }
                                        },
                                        onLongPressRelease = {
                                            when (item) {
                                                is HomeItem.Folder -> onEditModeChange(true)
                                                else -> onAppMenuArmDismiss()
                                            }
                                        },
                                        onDragStart = {
                                            onAppMenuDismiss()
                                            dragPage = page
                                            dragIndex = sourceIndex
                                            dragOffset = Offset.Zero
                                            hoverIndex = sourceIndex
                                            overRemove = false
                                            folderPreviewTarget = -1
                                            dragAnchor = cellCenters[index] ?: Offset.Zero
                                            dragFinger = dragAnchor
                                        },
                                        onDragEnd = {
                                            finishHomeDrag(
                                                editMode = false,
                                                fromPage = dragPage,
                                                fromIndex = dragIndex,
                                                hoverIndex = hoverIndex,
                                                dropPos = dragFinger,
                                                currentPage = pagerState.currentPage,
                                                itemsOnSource = state.pages.getOrElse(dragPage) { emptyList() },
                                                itemsOnDrop = state.pages.getOrElse(pagerState.currentPage) { emptyList() },
                                                cellCenters = cellCenters,
                                                folderPreviewTarget = folderPreviewTarget,
                                                removeZone = removeZone,
                                                density = density,
                                                onRemoveHomeItem = onRemoveHomeItem,
                                                onSwapHomeItems = onSwapHomeItems,
                                                onMoveHomeItem = onMoveHomeItem,
                                                onCreateFolder = onCreateFolder,
                                                onAddAppToFolder = onAddAppToFolder
                                            )
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                            overRemove = false
                                            folderPreviewTarget = -1
                                        },
                                        onDragCancel = {
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                            overRemove = false
                                            folderPreviewTarget = -1
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            dragFinger = dragAnchor + dragOffset
                                            hoverIndex = nearestSlotIndex(dragFinger, cellCenters)
                                        }
                                    )
                                }
                                .pointerInput(state.isEditMode, page, sourceIndex, item) {
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
                                .pointerInput(state.isEditMode, page, sourceIndex) {
                                    if (!state.isEditMode) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragPage = page
                                            dragIndex = sourceIndex
                                            dragOffset = Offset.Zero
                                            hoverIndex = sourceIndex
                                            overRemove = false
                                            folderPreviewTarget = -1
                                            dragAnchor = cellCenters[index] ?: Offset.Zero
                                            dragFinger = dragAnchor
                                        },
                                        onDragEnd = {
                                            finishHomeDrag(
                                                editMode = true,
                                                fromPage = dragPage,
                                                fromIndex = dragIndex,
                                                hoverIndex = hoverIndex,
                                                dropPos = dragFinger,
                                                currentPage = pagerState.currentPage,
                                                itemsOnSource = state.pages.getOrElse(dragPage) { emptyList() },
                                                itemsOnDrop = state.pages.getOrElse(pagerState.currentPage) { emptyList() },
                                                cellCenters = cellCenters,
                                                folderPreviewTarget = folderPreviewTarget,
                                                removeZone = removeZone,
                                                density = density,
                                                onRemoveHomeItem = onRemoveHomeItem,
                                                onSwapHomeItems = onSwapHomeItems,
                                                onMoveHomeItem = onMoveHomeItem,
                                                onCreateFolder = onCreateFolder,
                                                onAddAppToFolder = onAddAppToFolder
                                            )
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                            overRemove = false
                                            folderPreviewTarget = -1
                                        },
                                        onDragCancel = {
                                            dragIndex = -1
                                            dragPage = -1
                                            dragOffset = Offset.Zero
                                            hoverIndex = -1
                                            overRemove = false
                                            folderPreviewTarget = -1
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            val pos = dragAnchor + dragOffset
                                            dragFinger = pos
                                            overRemove = removeZone.contains(pos)
                                            val from = dragIndex
                                            val dItem = state.pages
                                                .getOrElse(dragPage) { emptyList() }
                                                .getOrNull(from)
                                            val threshold = with(density) { 56.dp.toPx() }
                                            val target = cellCenters
                                                .minByOrNull { (_, c) ->
                                                    hypot(
                                                        (c.x - pos.x).toDouble(),
                                                        (c.y - pos.y).toDouble()
                                                    )
                                                }
                                            val nearFolderMerge = target != null &&
                                                dItem is HomeItem.App &&
                                                dragPage == pagerState.currentPage
                                            if (nearFolderMerge) {
                                                val dist = hypot(
                                                    (target!!.value.x - pos.x).toDouble(),
                                                    (target.value.y - pos.y).toDouble()
                                                )
                                                val tItem = displayItems.getOrNull(target.key)
                                                if (dist < threshold &&
                                                    (tItem is HomeItem.App || tItem is HomeItem.Folder) &&
                                                    !sameHomeItem(dItem, tItem)
                                                ) {
                                                    // Map display target → source index for merge
                                                    folderPreviewTarget = when (tItem) {
                                                        is HomeItem.App -> rawItems.indexOfFirst {
                                                            it is HomeItem.App && it.key == tItem.key
                                                        }
                                                        is HomeItem.Folder -> rawItems.indexOfFirst {
                                                            it is HomeItem.Folder && it.id == tItem.id
                                                        }
                                                        else -> -1
                                                    }
                                                    hoverIndex = -1
                                                } else {
                                                    folderPreviewTarget = -1
                                                    hoverIndex = target.key
                                                }
                                            } else {
                                                folderPreviewTarget = -1
                                                hoverIndex = target?.key ?: -1
                                            }
                                        }
                                    )
                                }
                        ) {
                            when (item) {
                                is HomeItem.App -> {
                                    val app = state.appsByKey[item.key]
                                    if (app != null) {
                                        val showFolderPreview =
                                            state.isEditMode &&
                                                folderPreviewTarget == sourceIndex &&
                                                dragPage == page &&
                                                dragIndex >= 0
                                        val previewApps = if (showFolderPreview) {
                                            val dragged = state.pages
                                                .getOrElse(dragPage) { emptyList() }
                                                .getOrNull(dragIndex) as? HomeItem.App
                                            val draggedApp = dragged?.let { state.appsByKey[it.key] }
                                            listOfNotNull(app, draggedApp)
                                        } else {
                                            emptyList()
                                        }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (showFolderPreview && previewApps.size >= 2) {
                                                FolderMergePreview(
                                                    apps = previewApps,
                                                    iconScale = state.iconScale,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            } else {
                                                AppIcon(
                                                    app = app,
                                                    showLabel = state.showLabels,
                                                    iconScale = state.iconScale,
                                                    onClick = { onLaunchApp(app) },
                                                    onLongClick = {},
                                                    longPressEnabled = false,
                                                    editMode = state.isEditMode,
                                                    selected = item.key in selectedKeys,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .alpha(if (dragging) 0f else 1f)
                                                        .scale(if (dragging && overRemove) 0.88f else 1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                is HomeItem.Folder -> {
                                    val folderApps = state.folders[item.id]?.appKeys
                                        ?.mapNotNull { state.appsByKey[it] }
                                        .orEmpty()
                                    val showFolderPreview =
                                        state.isEditMode &&
                                            folderPreviewTarget == sourceIndex &&
                                            dragPage == page &&
                                            dragIndex >= 0
                                    val previewApps = if (showFolderPreview) {
                                        val dragged = state.pages
                                            .getOrElse(dragPage) { emptyList() }
                                            .getOrNull(dragIndex) as? HomeItem.App
                                        val draggedApp = dragged?.let { state.appsByKey[it.key] }
                                        (folderApps + listOfNotNull(draggedApp)).take(9)
                                    } else {
                                        folderApps
                                    }
                                    FolderIcon(
                                        name = state.folders[item.id]?.name ?: "Folder",
                                        apps = previewApps,
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
                                        handlePresses = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (dragging) 0f else 1f)
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
                    .padding(bottom = 10.dp)
                    .height(26.dp)
                    .pointerInput(state.isEditMode) {
                        if (state.isEditMode) return@pointerInput
                        detectTapGestures(onLongPress = { onEditModeChange(true) })
                    },
                contentAlignment = Alignment.Center
            ) {
                // Search & dots share the screen center. Assistant sits to the right of Search
                // and is NOT part of the centered group (so Search doesn't shift when dots show).
                var showDots by remember { mutableStateOf(false) }
                var searchWidthPx by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(pagerState, pageCount, state.isEditMode) {
                    if (state.isEditMode) {
                        showDots = true
                        return@LaunchedEffect
                    }
                    if (pageCount <= 1) {
                        showDots = false
                        return@LaunchedEffect
                    }
                    snapshotFlow {
                        pagerState.isScrollInProgress ||
                            abs(pagerState.currentPageOffsetFraction) > 0.001f
                    }
                        .distinctUntilChanged()
                        .collect { paging ->
                            if (paging) {
                                showDots = true
                            } else {
                                delay(500)
                                val stillPaging = pagerState.isScrollInProgress ||
                                    abs(pagerState.currentPageOffsetFraction) > 0.001f
                                if (!stillPaging) showDots = false
                            }
                        }
                }
                val dotsUi = showDots || state.isEditMode
                val dotsWidth = (24 + pageCount * 14).dp
                val dotsAlpha by animateFloatAsState(
                    targetValue = if (dotsUi) 1f else 0f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "dotsAlpha"
                )
                val searchAlpha by animateFloatAsState(
                    targetValue = if (dotsUi) 0f else 1f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "searchAlpha"
                )

                // Dots — dead-center of the screen
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = dotsAlpha }
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .height(26.dp)
                            .width(dotsWidth.coerceAtLeast(52.dp)),
                        cornerRadius = 99.dp,
                        strong = true,
                        enableSheen = true,
                        enableRefraction = true
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PageDots(
                                pageCount = pageCount,
                                current = pagerState.currentPage,
                                onDotClick = {
                                    scope.launch { pagerState.animateScrollToPage(it) }
                                }
                            )
                        }
                    }
                }

                // Search pill — same center as dots (Assistant is outside this box)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .onGloballyPositioned { searchWidthPx = it.size.width.toFloat() }
                        .graphicsLayer { alpha = searchAlpha }
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .height(26.dp)
                            .wrapContentWidth()
                            .clickable(
                                enabled = !dotsUi,
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
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.sp,
                                    lineHeight = 11.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Assistant — parked to the right of the centered Search pill
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset {
                            val gap = with(density) { 7.dp.roundToPx() }
                            val halfAssistant = with(density) { 13.dp.roundToPx() }
                            val w = if (searchWidthPx > 1f) {
                                searchWidthPx
                            } else {
                                with(density) { 64.dp.toPx() }
                            }
                            IntOffset((w / 2f + gap + halfAssistant).roundToInt(), 0)
                        }
                        .graphicsLayer { alpha = searchAlpha }
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(
                                enabled = !dotsUi,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { assistantOpen = true }
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
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Footer slot: min height keeps search from jumping; no max so the dock isn't squashed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 136.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !state.isEditMode,
                    enter = fadeIn(tween(260)) +
                        slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 4 },
                    exit = fadeOut(tween(180)) +
                        slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 4 },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    DockBar(
                        apps = state.dockApps,
                        iconScale = state.iconScale,
                        showLabels = state.dockLabels,
                        onAppClick = onLaunchApp,
                        onAppLongClick = onAppLongClick,
                        onAppMenuDismiss = onAppMenuDismiss,
                        onAppMenuArmDismiss = onAppMenuArmDismiss,
                        onSwapDockItems = onSwapDockItems,
                        onMoveDockAppToHome = { app ->
                            onMoveDockAppToHome(app, pagerState.currentPage)
                            dockDragApp = null
                            dockDragFinger = null
                        },
                        onDockDragChanged = { app, finger ->
                            dockDragApp = app
                            dockDragFinger = finger
                        },
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
                val densityFooter = LocalDensity.current
                // From the bottom edge up into place.
                val footerFrom = remember(densityFooter) {
                    with(densityFooter) { IntOffset(0, 120.dp.roundToPx()) }
                }
                GlassAwareSlide(
                    visible = state.isEditMode,
                    enterFrom = footerFrom,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            20.dp,
                            Alignment.CenterHorizontally
                        ),
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
                                context.startActivity(
                                    Intent(context, SettingsActivity::class.java)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Floating home drag ghost (escapes pager / grid clip)
        if (dragIndex >= 0 && dragPage >= 0) {
            val srcItems = state.pages.getOrElse(dragPage) { emptyList() }
            val dragItem = srcItems.getOrNull(dragIndex)
            val halfW = with(density) { 40.dp.toPx() }
            val halfH = with(density) { 48.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(30f)
                    .offset {
                        IntOffset(
                            (dragFinger.x - homeRootPos.x - halfW).roundToInt(),
                            (dragFinger.y - homeRootPos.y - halfH).roundToInt()
                        )
                    }
            ) {
                when (dragItem) {
                    is HomeItem.App -> {
                        val app = state.appsByKey[dragItem.key]
                        if (app != null) {
                            AppIcon(
                                app = app,
                                showLabel = state.showLabels,
                                iconScale = state.iconScale,
                                onClick = {},
                                onLongClick = {},
                                longPressEnabled = false,
                                editMode = false
                            )
                        }
                    }
                    is HomeItem.Folder -> {
                        FolderIcon(
                            name = state.folders[dragItem.id]?.name ?: "Folder",
                            apps = state.folders[dragItem.id]?.appKeys
                                ?.mapNotNull { state.appsByKey[it] }
                                .orEmpty(),
                            iconScale = state.iconScale,
                            editMode = false,
                            selected = false,
                            onClick = {},
                            onLongClick = {}
                        )
                    }
                    null -> Unit
                }
            }
        }

        // Dock drag ghost above footer / glass clip
        val dockGhost = dockDragApp
        val dockFinger = dockDragFinger
        if (dockGhost != null && dockFinger != null) {
            val halfW = with(density) { 40.dp.toPx() }
            val halfH = with(density) { 48.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(31f)
                    .offset {
                        IntOffset(
                            (dockFinger.x - homeRootPos.x - halfW).roundToInt(),
                            (dockFinger.y - homeRootPos.y - halfH).roundToInt()
                        )
                    }
            ) {
                AppIcon(
                    app = dockGhost,
                    showLabel = state.dockLabels,
                    iconScale = state.iconScale,
                    onClick = {},
                    onLongClick = {},
                    longPressEnabled = false,
                    editMode = false
                )
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
                    onLaunchApp = onLaunchApp,
                    onExtractApp = { app ->
                        onExtractAppFromFolder(folderId, app.key, pagerState.currentPage)
                    }
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
        PageManagerOverlay(
            visible = showPageManager,
            pages = state.pages,
            appsByKey = state.appsByKey,
            folders = state.folders,
            gridColumns = state.gridColumns,
            onDismiss = { showPageManager = false },
            onAddPage = onAddPage,
            onRemovePage = onRemovePage
        )

        AssistantOverlay(
            visible = assistantOpen,
            apps = state.apps,
            onDismiss = { assistantOpen = false },
            onLaunchApp = onLaunchApp,
            onOpenDrawer = {
                assistantOpen = false
                onOpenDrawer()
            },
            onSearchApps = { query ->
                assistantOpen = false
                onSearchApps(query)
            },
            onOpenSettings = {
                assistantOpen = false
                context.startActivity(Intent(context, SettingsActivity::class.java))
            },
            onEditHome = {
                assistantOpen = false
                onEditModeChange(true)
            }
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

/**
 * Slide via layout [offset] (not AnimatedVisibility's graphicsLayer) so
 * [GlassPanel] window position — and wallpaper sampling — tracks the motion.
 *
 * Offset travel is intentionally long + soft so the slide reads before fade finishes
 * (short springs used to finish while still invisible → looked like a hard appear).
 */
@Composable
private fun GlassAwareSlide(
    visible: Boolean,
    enterFrom: IntOffset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Start off-screen so the first composed frame isn't already at rest.
    val offset = remember(enterFrom) {
        Animatable(enterFrom, IntOffset.VectorConverter)
    }
    val alpha = remember { Animatable(0f) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible, enterFrom) {
        if (visible) {
            show = true
            offset.snapTo(enterFrom)
            alpha.snapTo(0f)
            // Fade up quickly so the long slide is visible for most of the travel.
            launch { alpha.animateTo(1f, tween(160)) }
            offset.animateTo(
                targetValue = IntOffset.Zero,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else if (show) {
            launch {
                offset.animateTo(
                    targetValue = enterFrom,
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                )
            }
            alpha.animateTo(0f, tween(160))
            show = false
        }
    }

    if (show || visible) {
        Box(
            modifier = modifier
                .graphicsLayer { this.alpha = alpha.value }
                .offset { offset.value }
        ) {
            content()
        }
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
        enableSheen = true,
        enableRefraction = true,
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
        modifier = modifier
            .size(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 28.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = true,
        sampleWallpaper = true
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
    modifier: Modifier = Modifier,
    handlePresses: Boolean = true
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
                if (editMode || !handlePresses) Modifier
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
                    .liquidGlassStroke(shape = shape, strong = true)
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
                        .background(if (selected) IosBlue else Color(0xE63A3A3C)),
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
    onLaunchApp: (AppInfo) -> Unit,
    onExtractApp: (AppInfo) -> Unit
) {
    val panelWidth = 312.dp
    val titleBlock = 44.dp
    val gridHeight = 268.dp
    val panelHeight = titleBlock + gridHeight + 36.dp
    val density = LocalDensity.current

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var iconCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    // Hide folder chrome after dragging outside for 0.5s, but keep gesture alive
    var chromeHidden by remember { mutableStateOf(false) }
    var outsideSinceMs by remember { mutableStateOf<Long?>(null) }

    val progress = remember { Animatable(0f) }
    val chromeAlpha = remember { Animatable(1f) }
    LaunchedEffect(closing) {
        if (!closing) {
            progress.snapTo(0f)
            chromeAlpha.snapTo(1f)
            chromeHidden = false
            outsideSinceMs = null
            progress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        } else {
            progress.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            onCloseFinished()
        }
    }
    LaunchedEffect(chromeHidden) {
        if (chromeHidden) {
            chromeAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        } else if (!closing) {
            chromeAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
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
    val chrome = chromeAlpha.value

    fun updateOutside(drop: Offset?) {
        val outside = drop != null &&
            panelBounds.width > 1f &&
            !panelBounds.expandBy(12f).contains(drop)
        val now = android.os.SystemClock.uptimeMillis()
        if (outside) {
            val since = outsideSinceMs ?: now.also { outsideSinceMs = it }
            if (!chromeHidden && now - since >= 500L) {
                chromeHidden = true
            }
        } else {
            outsideSinceMs = null
            if (chromeHidden && dragIndex >= 0) {
                chromeHidden = false
            }
        }
    }

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
                .background(Color.Black.copy(alpha = 0.55f * t * chrome))
                .then(
                    if (chromeHidden) Modifier
                    else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRequestClose
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(chrome),
            contentAlignment = Alignment.Center
        ) {
            val settled = t >= 0.995f
            if (!settled) {
                // Morph with cheap frost plate — wallpaper glass can't track graphicsLayer morph.
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = translateX
                        translationY = translateY
                        alpha = (0.35f + 0.65f * t).coerceIn(0f, 1f)
                    }
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .width(panelWidth)
                            .height(panelHeight),
                        cornerRadius = 28.dp,
                        strong = true,
                        enableSheen = false,
                        enableRefraction = false,
                        sampleWallpaper = false
                    ) {
                        FolderPanelContent(
                            name = name,
                            titleBlock = titleBlock,
                            gridHeight = gridHeight,
                            apps = apps,
                            iconScale = iconScale,
                            dragIndex = -1,
                            closing = true, // no drag mid-morph
                            onLaunchApp = onLaunchApp,
                            onRequestClose = onRequestClose,
                            onIconCenter = { _, _ -> },
                            onDragStart = { },
                            onDrag = { _, _ -> },
                            onDragEnd = { },
                            onDragCancel = { }
                        )
                    }
                }
            } else {
                // Fresh glass at layout position (no morph layer) — correct wallpaper sample.
                GlassPanel(
                    modifier = Modifier
                        .width(panelWidth)
                        .height(panelHeight)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            panelBounds = Rect(
                                pos.x,
                                pos.y,
                                pos.x + coords.size.width,
                                pos.y + coords.size.height
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    cornerRadius = 28.dp,
                    strong = true,
                    enableSheen = false,
                    enableRefraction = true,
                    sampleWallpaper = true
                ) {
                    FolderPanelContent(
                        name = name,
                        titleBlock = titleBlock,
                        gridHeight = gridHeight,
                        apps = apps,
                        iconScale = iconScale,
                        dragIndex = dragIndex,
                        closing = closing,
                        onLaunchApp = onLaunchApp,
                        onRequestClose = onRequestClose,
                        onIconCenter = { i, c -> iconCenters = iconCenters + (i to c) },
                        onDragStart = { i ->
                            dragIndex = i
                            dragOffset = Offset.Zero
                            outsideSinceMs = null
                            chromeHidden = false
                        },
                        onDrag = { i, amount ->
                            dragOffset += amount
                            updateOutside(iconCenters[i]?.plus(dragOffset))
                        },
                        onDragEnd = { app ->
                            val drop = iconCenters[dragIndex]?.plus(dragOffset)
                            val outside = drop != null &&
                                panelBounds.width > 1f &&
                                !panelBounds.expandBy(12f).contains(drop)
                            if (outside || chromeHidden) {
                                onExtractApp(app)
                                onRequestClose()
                            } else {
                                chromeHidden = false
                            }
                            dragIndex = -1
                            dragOffset = Offset.Zero
                            outsideSinceMs = null
                        },
                        onDragCancel = {
                            dragIndex = -1
                            dragOffset = Offset.Zero
                            outsideSinceMs = null
                            chromeHidden = false
                        }
                    )
                }
            }
        }

        // Ghost icon above the clipped folder panel so drag can leave the glass
        val floating = apps.getOrNull(dragIndex)
        val anchor = iconCenters[dragIndex]
        if (floating != null && dragIndex >= 0 && anchor != null) {
            val halfW = with(density) { 40.dp.toPx() }
            val halfH = with(density) { 48.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(20f)
                    .offset {
                        IntOffset(
                            (anchor.x + dragOffset.x - rootPos.x - halfW).roundToInt(),
                            (anchor.y + dragOffset.y - rootPos.y - halfH).roundToInt()
                        )
                    }
            ) {
                AppIcon(
                    app = floating,
                    iconScale = iconScale * 0.92f,
                    showLabel = true,
                    onClick = {},
                    onLongClick = {},
                    longPressEnabled = false
                )
            }
        }
    }
}

@Composable
private fun FolderPanelContent(
    name: String,
    titleBlock: androidx.compose.ui.unit.Dp,
    gridHeight: androidx.compose.ui.unit.Dp,
    apps: List<AppInfo>,
    iconScale: Float,
    dragIndex: Int,
    closing: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onRequestClose: () -> Unit,
    onIconCenter: (Int, Offset) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Int, Offset) -> Unit,
    onDragEnd: (AppInfo) -> Unit,
    onDragCancel: () -> Unit
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
                .height(gridHeight),
            userScrollEnabled = dragIndex < 0
        ) {
            items(apps.size) { i ->
                val app = apps[i]
                val dragging = dragIndex == i
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val c = coords.positionInWindow() + Offset(
                                coords.size.width / 2f,
                                coords.size.height / 2f
                            )
                            onIconCenter(i, c)
                        }
                        .pointerInput(i, apps.size, closing) {
                            if (closing) return@pointerInput
                            detectLongPressMenuOrDrag(
                                onTap = {
                                    onRequestClose()
                                    onLaunchApp(app)
                                },
                                onLongPress = { },
                                onDragStart = { onDragStart(i) },
                                onDrag = { change, amount ->
                                    change.consume()
                                    onDrag(i, amount)
                                },
                                onDragCancel = onDragCancel,
                                onDragEnd = { onDragEnd(app) }
                            )
                        }
                        .alpha(if (dragging) 0f else 1f)
                ) {
                    AppIcon(
                        app = app,
                        iconScale = iconScale * 0.92f,
                        showLabel = true,
                        onClick = {
                            onRequestClose()
                            onLaunchApp(app)
                        },
                        onLongClick = {},
                        longPressEnabled = false
                    )
                }
            }
        }
    }
}

private fun sameHomeItem(a: HomeItem, b: HomeItem): Boolean = when {
    a is HomeItem.App && b is HomeItem.App -> a.key == b.key
    a is HomeItem.Folder && b is HomeItem.Folder -> a.id == b.id
    else -> false
}

private fun reorderPreview(items: List<HomeItem>, from: Int, to: Int): List<HomeItem> {
    if (from !in items.indices) return items
    if (to < 0) return items
    if (from == to) return items
    val next = items.toMutableList()
    val item = next.removeAt(from)
    next.add(to.coerceIn(0, next.size), item)
    return next
}

private fun nearestSlotIndex(pos: Offset, centers: Map<Int, Offset>): Int {
    if (centers.isEmpty()) return -1
    return centers.minByOrNull { (_, c) ->
        hypot((c.x - pos.x).toDouble(), (c.y - pos.y).toDouble())
    }?.key ?: -1
}

private fun finishHomeDrag(
    editMode: Boolean,
    fromPage: Int,
    fromIndex: Int,
    hoverIndex: Int,
    dropPos: Offset,
    currentPage: Int,
    itemsOnSource: List<HomeItem>,
    itemsOnDrop: List<HomeItem>,
    cellCenters: Map<Int, Offset>,
    folderPreviewTarget: Int,
    removeZone: Rect,
    density: androidx.compose.ui.unit.Density,
    onRemoveHomeItem: (page: Int, index: Int) -> Unit,
    onSwapHomeItems: (page: Int, a: Int, b: Int) -> Unit,
    onMoveHomeItem: (fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int) -> Unit,
    onCreateFolder: (page: Int, target: Int, dragged: Int) -> Unit,
    onAddAppToFolder: (page: Int, folderIndex: Int, appIndex: Int) -> Unit
) {
    if (fromIndex < 0 || fromPage < 0) return
    val threshold = with(density) { 56.dp.toPx() }

    if (editMode && removeZone.width > 1f && removeZone.contains(dropPos)) {
        onRemoveHomeItem(fromPage, fromIndex)
        return
    }

    val dropPage = currentPage
    val samePage = dropPage == fromPage

    // Edit-mode folder merge takes priority when preview was active
    if (editMode && samePage && folderPreviewTarget >= 0) {
        val tItem = itemsOnSource.getOrNull(folderPreviewTarget)
        val dItem = itemsOnSource.getOrNull(fromIndex)
        when {
            tItem is HomeItem.App && dItem is HomeItem.App ->
                onCreateFolder(fromPage, folderPreviewTarget, fromIndex)
            tItem is HomeItem.Folder && dItem is HomeItem.App ->
                onAddAppToFolder(fromPage, folderPreviewTarget, fromIndex)
            tItem is HomeItem.App && dItem is HomeItem.Folder ->
                onAddAppToFolder(fromPage, fromIndex, folderPreviewTarget)
            else -> Unit
        }
        return
    }

    // Live-reorder commit: move into the hovered slot
    if (samePage && hoverIndex >= 0 && hoverIndex != fromIndex) {
        onMoveHomeItem(fromPage, fromIndex, dropPage, hoverIndex.coerceIn(0, itemsOnSource.size))
        return
    }

    if (!samePage) {
        val insert = if (hoverIndex >= 0) {
            hoverIndex.coerceIn(0, itemsOnDrop.size)
        } else {
            val target = cellCenters.minByOrNull { (_, c) ->
                hypot((c.x - dropPos.x).toDouble(), (c.y - dropPos.y).toDouble())
            }
            val dist = if (target != null) {
                hypot(
                    (target.value.x - dropPos.x).toDouble(),
                    (target.value.y - dropPos.y).toDouble()
                )
            } else {
                Double.MAX_VALUE
            }
            if (target != null && dist < threshold) target.key.coerceIn(0, itemsOnDrop.size)
            else itemsOnDrop.size
        }
        onMoveHomeItem(fromPage, fromIndex, dropPage, insert)
    }
}

@Composable
private fun FolderMergePreview(
    apps: List<AppInfo>,
    iconScale: Float,
    modifier: Modifier = Modifier
) {
    val appearance = LocalIconAppearance.current
    val effectiveScale = appearance.scale
    val size = (56 * effectiveScale * iconScale.coerceIn(0.7f, 1.3f)).dp
    val radiusRatio = appearance.cornerRadiusRatio
    val shape = remember(appearance.shapePercent) {
        SmoothCornerShape(percent = appearance.shapePercent.coerceAtLeast(1))
    }
    val colorFilter = remember(appearance.theme, appearance.tintHue, appearance.tintAlpha) {
        appearance.colorFilter()
    }
    val previews = remember(apps.map { it.key }, radiusRatio) {
        apps.take(4).map { it.icon.toCachedBitmap(64, cornerRadiusRatio = radiusRatio).asImageBitmap() }
    }
    val pad = 6.dp
    val gap = 3.dp
    val cell = (size - pad * 2 - gap) / 2
    Column(
        modifier = modifier
            .width(80.dp)
            .padding(vertical = 6.dp)
            .scale(1.08f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .liquidGlassStroke(shape = shape, strong = true)
                .clip(shape)
                .background(Color(0x88FFFFFF))
                .padding(pad)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
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
                                Spacer(modifier = Modifier.size(cell))
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "Folder",
            style = MaterialTheme.typography.labelSmall,
            color = VoidMist,
            maxLines = 1
        )
    }
}

private fun Rect.expandBy(amount: Float): Rect = Rect(
    left - amount,
    top - amount,
    right + amount,
    bottom + amount
)
