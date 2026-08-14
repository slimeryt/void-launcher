package com.voidlauncher.app.ui.drawer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.gestures.detectLongPressMenuOrDrag
import com.voidlauncher.app.util.PendingLaunchBounds
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AppDrawer(
    visible: Boolean,
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onAppMenuDismiss: () -> Unit = {},
    onAppMenuArmDismiss: () -> Unit = {},
    onAddAppToHome: (AppInfo) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onClose)

    val gridState = rememberLazyGridState()
    var headerDrag by remember { mutableFloatStateOf(0f) }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var iconCenters by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }
    var iconBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var chromeHidden by remember { mutableStateOf(false) }
    var outsideSinceMs by remember { mutableStateOf<Long?>(null) }
    val chromeAlpha = remember { Animatable(1f) }
    // Defer heavy grid until the open animation has started — avoids hitching the UI thread
    var gridReady by remember { mutableStateOf(false) }

    LaunchedEffect(visible, state.drawerFocusSearch) {
        if (visible && state.drawerFocusSearch) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(90)
            gridReady = true
        } else {
            gridReady = false
            dragIndex = -1
            dragOffset = Offset.Zero
            chromeHidden = false
            outsideSinceMs = null
            chromeAlpha.snapTo(1f)
        }
    }

    LaunchedEffect(chromeHidden) {
        if (chromeHidden) {
            chromeAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        } else if (visible) {
            chromeAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    fun updateOutside(drop: Offset?) {
        val outside = drop != null &&
            panelBounds.width > 1f &&
            !panelBounds.expandBy(24f).contains(drop)
        val now = SystemClock.uptimeMillis()
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

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 5 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 5 },
        modifier = modifier
    ) {
        val chrome = chromeAlpha.value
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootPos = it.positionInWindow() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x6605060A).copy(alpha = 0.4f * chrome))
                    .then(
                        if (chromeHidden) Modifier
                        else Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose
                        )
                    )
            )

            GlassPanel(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .alpha(chrome)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        panelBounds = Rect(
                            pos.x,
                            pos.y,
                            pos.x + coords.size.width,
                            pos.y + coords.size.height
                        )
                    },
                cornerRadius = 36.dp,
                strong = true,
                enableSheen = false,
                enableRefraction = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (headerDrag > 80f) onClose()
                                        headerDrag = 0f
                                    },
                                    onDragCancel = { headerDrag = 0f },
                                    onVerticalDrag = { _, amount ->
                                        if (amount > 0f) headerDrag += amount
                                        else headerDrag = (headerDrag + amount).coerceAtLeast(0f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(VoidMuted.copy(alpha = 0.55f), CapsuleShape)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Apps",
                            style = MaterialTheme.typography.headlineMedium,
                            color = VoidMist
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VoidMist
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isEditMode) {
                        Text(
                            text = "Long-press an app to add it back to Home",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VoidMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        strong = true,
                        enableSheen = false,
                        enableRefraction = false
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = VoidMuted
                            )
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                cursorBrush = SolidColor(VoidCyan),
                                textStyle = MaterialTheme.typography.titleMedium.copy(color = VoidMist),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(searchFocus),
                                decorationBox = { inner ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search apps",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = VoidMuted
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        state.isLoading || !gridReady -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoading || visible) {
                                    CircularProgressIndicator(color = VoidCyan)
                                }
                            }
                        }
                        state.filteredApps.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.searchQuery.isBlank()) "No apps" else "No matches",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VoidMuted
                                )
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(state.gridColumns),
                                state = gridState,
                                contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                userScrollEnabled = dragIndex < 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = state.filteredApps,
                                    key = { _, app -> app.key },
                                    contentType = { _, _ -> "app" }
                                ) { index, app ->
                                    val dragging = dragIndex == index
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                val pos = coords.positionInWindow()
                                                val w = coords.size.width.toFloat()
                                                val h = coords.size.height.toFloat()
                                                iconBounds = iconBounds + (
                                                    index to Rect(pos.x, pos.y, pos.x + w, pos.y + h)
                                                    )
                                                iconCenters = iconCenters + (
                                                    index to Offset(pos.x + w / 2f, pos.y + h / 2f)
                                                    )
                                            }
                                            .pointerInput(app.key, state.isEditMode, index) {
                                                if (state.isEditMode) return@pointerInput
                                                detectLongPressMenuOrDrag(
                                                    onTap = {
                                                        iconBounds[index]?.let { r ->
                                                            PendingLaunchBounds.rect =
                                                                android.graphics.Rect(
                                                                    r.left.toInt(),
                                                                    r.top.toInt(),
                                                                    r.right.toInt(),
                                                                    r.bottom.toInt()
                                                                )
                                                        }
                                                        onLaunchApp(app)
                                                    },
                                                    onLongPress = {
                                                        onAppLongClick(
                                                            app,
                                                            iconBounds[index] ?: Rect.Zero
                                                        )
                                                    },
                                                    onLongPressRelease = {
                                                        onAppMenuArmDismiss()
                                                    },
                                                    onDragStart = {
                                                        onAppMenuDismiss()
                                                        dragIndex = index
                                                        dragOffset = Offset.Zero
                                                        outsideSinceMs = null
                                                        chromeHidden = false
                                                    },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        dragOffset += amount
                                                        updateOutside(
                                                            iconCenters[index]?.plus(dragOffset)
                                                        )
                                                    },
                                                    onDragCancel = {
                                                        dragIndex = -1
                                                        dragOffset = Offset.Zero
                                                        outsideSinceMs = null
                                                        chromeHidden = false
                                                    },
                                                    onDragEnd = {
                                                        val drop = iconCenters[index]?.plus(dragOffset)
                                                        val outside = drop != null &&
                                                            panelBounds.width > 1f &&
                                                            !panelBounds.expandBy(24f).contains(drop)
                                                        if (outside || chromeHidden) {
                                                            onAddAppToHome(app)
                                                            onClose()
                                                        } else {
                                                            chromeHidden = false
                                                        }
                                                        dragIndex = -1
                                                        dragOffset = Offset.Zero
                                                        outsideSinceMs = null
                                                    }
                                                )
                                            }
                                            .alpha(if (dragging) 0f else 1f)
                                    ) {
                                        AppIcon(
                                            app = app,
                                            showLabel = state.showLabels,
                                            iconScale = state.iconScale,
                                            onClick = {
                                                if (state.isEditMode) onAddAppToHome(app)
                                                else onLaunchApp(app)
                                            },
                                            onLongClick = {
                                                if (state.isEditMode) onAddAppToHome(app)
                                            },
                                            longPressEnabled = state.isEditMode,
                                            editMode = state.isEditMode,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val floating = state.filteredApps.getOrNull(dragIndex)
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
                        showLabel = state.showLabels,
                        iconScale = state.iconScale,
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
