package com.voidlauncher.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.ui.components.AppActionsSheet
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.LocalHiddenAppKey
import com.voidlauncher.app.ui.drawer.AppDrawer
import com.voidlauncher.app.ui.home.HomeScreen
import com.voidlauncher.app.ui.icons.IconAppearance
import com.voidlauncher.app.ui.icons.IconEditorPanel
import com.voidlauncher.app.ui.icons.IconTheme
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.ui.shade.ControlCenter
import com.voidlauncher.app.ui.shade.ControlCenterController
import com.voidlauncher.app.ui.shade.NotificationCenter
import com.voidlauncher.app.viewmodel.LauncherUiState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LauncherRoot(
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDrawerOpenChange: (Boolean) -> Unit,
    onOpenDrawerSearch: () -> Unit,
    onNotificationCenterOpenChange: (Boolean) -> Unit = {},
    onControlCenterOpenChange: (Boolean) -> Unit = {},
    onEditModeChange: (Boolean) -> Unit,
    onRemoveHomeItem: (page: Int, index: Int) -> Unit,
    onSwapHomeItems: (page: Int, a: Int, b: Int) -> Unit,
    onMoveHomeItem: (fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int) -> Unit,
    onSwapDockItems: (a: Int, b: Int) -> Unit,
    onMoveDockAppToHome: (AppInfo, page: Int) -> Unit,
    onCreateFolder: (page: Int, target: Int, dragged: Int) -> Unit,
    onAddAppToFolder: (page: Int, folderIndex: Int, appIndex: Int) -> Unit,
    onExtractAppFromFolder: (folderId: String, appKey: String, pageIndex: Int) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: (pageIndex: Int) -> Unit,
    onApplyIconAppearance: (IconAppearance) -> Unit,
    onIconEditorOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemUi = rememberSystemUiController()
    SideEffect {
        systemUi.setSystemBarsColor(Color.Transparent, darkIcons = false)
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val controlCenter = remember(context) {
        ControlCenterController(context, activity?.window)
    }
    val scope = rememberCoroutineScope()
    val ccExpansion = remember { Animatable(0f) }
    var ccDragging by remember { mutableStateOf(false) }

    LaunchedEffect(state.isControlCenterOpen) {
        if (ccDragging) return@LaunchedEffect
        val target = if (state.isControlCenterOpen) 1f else 0f
        if (kotlin.math.abs(ccExpansion.value - target) > 0.01f) {
            ccExpansion.animateTo(
                target,
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.86f)
            )
        }
    }

    var actionApp by remember { mutableStateOf<AppInfo?>(null) }
    var focusBounds by remember { mutableStateOf<Rect?>(null) }
    /** False while finger is still down after long-press so hold→drag isn't blocked by the scrim. */
    var menuOutsideDismiss by remember { mutableStateOf(true) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var iconDraft by remember { mutableStateOf(IconAppearance.Default) }

    fun openAppMenu(app: AppInfo, bounds: Rect?) {
        actionApp = app
        focusBounds = bounds?.takeIf { it.width > 1f && it.height > 1f }
        // Finger is still down — no full-screen dismiss layer yet.
        menuOutsideDismiss = false
    }

    fun armAppMenuDismiss() {
        if (actionApp != null) menuOutsideDismiss = true
    }

    fun dismissAppMenu() {
        actionApp = null
        focusBounds = null
        menuOutsideDismiss = true
    }

    LaunchedEffect(state.iconEditorOpen) {
        if (state.iconEditorOpen) {
            iconDraft = IconAppearance(
                theme = IconTheme.fromKey(state.iconTheme),
                cornerRadiusPercent = state.iconCornerRadiusPercent,
                tintHue = state.iconTintHue,
                tintAlpha = state.iconTintAlpha,
                scale = state.iconScale
            )
        }
    }

    val committed = remember(
        state.iconTheme,
        state.iconCornerRadiusPercent,
        state.iconTintHue,
        state.iconTintAlpha,
        state.iconScale
    ) {
        IconAppearance(
            theme = IconTheme.fromKey(state.iconTheme),
            cornerRadiusPercent = state.iconCornerRadiusPercent,
            tintHue = state.iconTintHue,
            tintAlpha = state.iconTintAlpha,
            scale = state.iconScale
        )
    }
    val appearance = if (state.iconEditorOpen) iconDraft else committed

    BackHandler(enabled = state.iconEditorOpen) {
        onIconEditorOpenChange(false)
    }
    BackHandler(enabled = actionApp != null && !state.iconEditorOpen) {
        dismissAppMenu()
    }

    LaunchedEffect(state.isEditMode) {
        if (state.isEditMode) dismissAppMenu()
    }

    val hazeState = rememberHazeState()
    val menuOpen = actionApp != null
    // Blur/zoom only after finger lifts (menu sticky). Applying blur while still holding
    // rebuilds the home modifier tree and cancels the hold→drag pointer.
    val menuSettled = menuOpen && menuOutsideDismiss
    val homeFocusScale by animateFloatAsState(
        targetValue = if (menuSettled) 0.96f else 1f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "homeFocusZoom"
    )
    val homeBlur = when {
        menuSettled -> 20.dp
        else -> (48f * ccExpansion.value).dp
    }

    CompositionLocalProvider(
        LocalIconAppearance provides appearance,
        LocalHazeState provides hazeState
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { rootPos = it.positionInWindow() }
        ) {
            // Stable blur modifier (radius animates 0↔20) so the node isn't added/removed mid-hold.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(homeBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                if (menuSettled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = homeFocusScale
                            scaleY = homeFocusScale
                            transformOrigin = TransformOrigin.Center
                        }
                ) {
                    CompositionLocalProvider(
                        LocalHiddenAppKey provides if (menuSettled) actionApp?.key else null
                    ) {
                    HomeScreen(
                        state = state,
                        onLaunchApp = onLaunchApp,
                        onAppLongClick = { app, bounds -> openAppMenu(app, bounds) },
                        onAppMenuDismiss = { dismissAppMenu() },
                        onAppMenuArmDismiss = { armAppMenuDismiss() },
                        onOpenDrawer = { onDrawerOpenChange(true) },
                        onOpenDrawerSearch = onOpenDrawerSearch,
                        onOpenNotificationCenter = { onNotificationCenterOpenChange(true) },
                        onOpenControlCenter = {
                            ccDragging = false
                            onControlCenterOpenChange(true)
                        },
                        onControlCenterPull = { progress ->
                            ccDragging = true
                            scope.launch { ccExpansion.snapTo(progress.coerceIn(0f, 1f)) }
                        },
                        onControlCenterPullEnd = { open ->
                            ccDragging = false
                            onControlCenterOpenChange(open)
                            scope.launch {
                                ccExpansion.animateTo(
                                    if (open) 1f else 0f,
                                    spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = 0.86f
                                    )
                                )
                            }
                        },
                        onSearchApps = { query ->
                            onSearchQueryChange(query)
                            onOpenDrawerSearch()
                        },
                        onEditModeChange = onEditModeChange,
                        onRemoveHomeItem = onRemoveHomeItem,
                        onSwapHomeItems = onSwapHomeItems,
                        onMoveHomeItem = onMoveHomeItem,
                        onSwapDockItems = onSwapDockItems,
                        onMoveDockAppToHome = onMoveDockAppToHome,
                        onCreateFolder = onCreateFolder,
                        onAddAppToFolder = onAddAppToFolder,
                        onExtractAppFromFolder = onExtractAppFromFolder,
                        onAddPage = onAddPage,
                        onRemovePage = onRemovePage,
                        onAddAppToHome = { app, _ -> onAddAppToHome(app) },
                        modifier = Modifier.fillMaxSize()
                    )
                    }
                }
            }

            // Drawer outside the home blur/zoom layer so opening it stays snappy
            CompositionLocalProvider(
                LocalHiddenAppKey provides if (menuSettled) actionApp?.key else null
            ) {
            AppDrawer(
                visible = state.isDrawerOpen,
                state = state,
                onLaunchApp = onLaunchApp,
                onAppLongClick = { app, bounds -> openAppMenu(app, bounds) },
                onAppMenuDismiss = { dismissAppMenu() },
                onAppMenuArmDismiss = { armAppMenuDismiss() },
                onAddAppToHome = onAddAppToHome,
                onSearchQueryChange = onSearchQueryChange,
                onClose = { onDrawerOpenChange(false) },
                modifier = Modifier.fillMaxSize()
            )
            }

            NotificationCenter(
                visible = state.isNotificationCenterOpen,
                onClose = { onNotificationCenterOpenChange(false) },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(12f)
            )

            ControlCenter(
                visible = state.isControlCenterOpen || ccExpansion.value > 0.001f,
                expansion = ccExpansion.value,
                controller = controlCenter,
                onClose = { onControlCenterOpenChange(false) },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(12f)
            )

            // Sharp focused icon only once the menu is sticky (finger up). Showing it under
            // the still-down finger would steal the hold→drag pointer.
            val focused = actionApp
            val bounds = focusBounds
            if (menuSettled && focused != null && bounds != null) {
                CompositionLocalProvider(LocalHiddenAppKey provides null) {
                Box(
                    modifier = Modifier
                        .zIndex(8f)
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                (bounds.left - rootPos.x).roundToInt(),
                                (bounds.top - rootPos.y).roundToInt()
                            )
                        }
                ) {
                    AppIcon(
                        app = focused,
                        showLabel = state.showLabels,
                        iconScale = state.iconScale,
                        onClick = {},
                        onLongClick = {},
                        longPressEnabled = false
                    )
                }
                }
            }

            AppActionsSheet(
                app = actionApp,
                anchorBounds = focusBounds,
                outsideDismissEnabled = menuOutsideDismiss,
                onDismiss = { dismissAppMenu() },
                onFavorite = onToggleFavorite,
                onHide = onHideApp,
                onAppInfo = onAppInfo
            )

            if (state.iconEditorOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onIconEditorOpenChange(false) }
                        )
                )
                IconEditorPanel(
                    draft = iconDraft,
                    onDraftChange = { iconDraft = it },
                    onApply = {
                        onApplyIconAppearance(iconDraft)
                        onIconEditorOpenChange(false)
                    },
                    onDismiss = { onIconEditorOpenChange(false) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                )
            }
        }
    }
}
