package com.voidlauncher.app.ui.drawer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppDrawer(
    visible: Boolean,
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val gridState = rememberLazyGridState()
    var dragFromHeader by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            offsetY.snapTo(with(density) { 80.dp.toPx() })
            offsetY.animateTo(0f, spring(stiffness = 400f, dampingRatio = 0.85f))
        } else {
            offsetY.snapTo(0f)
        }
    }

    if (!visible) return

    fun dismiss() {
        scope.launch {
            val h = with(density) { 640.dp.toPx() }
            offsetY.animateTo(h, spring(stiffness = 500f))
            onClose()
        }
    }

    val nestedScroll = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // Pulling down while at top of list → dismiss drag
                if (available.y > 0f &&
                    gridState.firstVisibleItemIndex == 0 &&
                    gridState.firstVisibleItemScrollOffset == 0
                ) {
                    scope.launch {
                        offsetY.snapTo((offsetY.value + available.y).coerceAtLeast(0f))
                    }
                    return available
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value > with(density) { 100.dp.toPx() }) {
                    dismiss()
                    return available
                }
                if (offsetY.value > 0f) {
                    offsetY.animateTo(0f, spring(stiffness = 500f))
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE605060A))
            .nestedScroll(nestedScroll)
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .graphicsLayer {
                    val progress = (offsetY.value / with(density) { 400.dp.toPx() }).coerceIn(0f, 1f)
                    alpha = 1f - progress * 0.35f
                }
                .pointerInput(Unit) {
                    // Header / empty area swipe-down
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragFromHeader > 90f) dismiss()
                            else scope.launch { offsetY.animateTo(0f, spring(stiffness = 500f)) }
                            dragFromHeader = 0f
                        },
                        onVerticalDrag = { _, amount ->
                            if (amount > 0 &&
                                gridState.firstVisibleItemIndex == 0 &&
                                gridState.firstVisibleItemScrollOffset == 0
                            ) {
                                dragFromHeader += amount
                                scope.launch {
                                    offsetY.snapTo((offsetY.value + amount).coerceAtLeast(0f))
                                }
                            }
                        }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(VoidMuted.copy(alpha = 0.5f), RoundedPill)
                    )
                }

                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VoidMist
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    enableSheen = false,
                    enableRefraction = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            modifier = Modifier.weight(1f),
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
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VoidCyan)
                        }
                    }
                    state.filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.filteredApps,
                                key = { it.key },
                                contentType = { "app" }
                            ) { app ->
                                AppIcon(
                                    app = app,
                                    showLabel = state.showLabels,
                                    iconScale = state.iconScale,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onAppLongClick(app) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val RoundedPill = androidx.compose.foundation.shape.RoundedCornerShape(99.dp)
