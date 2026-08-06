package com.voidlauncher.app.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.voidlauncher.app.ui.components.CapsuleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppIcon
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState

@Composable
fun AppDrawer(
    visible: Boolean,
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
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

    LaunchedEffect(visible, state.drawerFocusSearch) {
        if (visible && state.drawerFocusSearch) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim — tap to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x6605060A))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )

            GlassPanel(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                cornerRadius = 36.dp,
                strong = true,
                enableSheen = true,
                enableRefraction = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Drag handle — swipe down to close (only this area steals vertical drag)
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
                        enableSheen = true,
                        enableRefraction = true
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
                        state.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = VoidCyan)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                                        onClick = {
                                            if (state.isEditMode) onAddAppToHome(app)
                                            else onLaunchApp(app)
                                        },
                                        onLongClick = {
                                            if (state.isEditMode) onAddAppToHome(app)
                                            else onAppLongClick(app)
                                        },
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
    }
}
