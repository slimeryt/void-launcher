package com.voidlauncher.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppActionsSheet
import com.voidlauncher.app.ui.drawer.AppDrawer
import com.voidlauncher.app.ui.home.HomeScreen
import com.voidlauncher.app.ui.icons.IconAppearance
import com.voidlauncher.app.ui.icons.IconEditorPanel
import com.voidlauncher.app.ui.icons.IconTheme
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.viewmodel.LauncherUiState

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
    onEditModeChange: (Boolean) -> Unit,
    onRemoveHomeItem: (page: Int, index: Int) -> Unit,
    onSwapHomeItems: (page: Int, a: Int, b: Int) -> Unit,
    onCreateFolder: (page: Int, target: Int, dragged: Int) -> Unit,
    onAddAppToFolder: (page: Int, folderIndex: Int, appIndex: Int) -> Unit,
    onAddPage: () -> Unit,
    onApplyIconAppearance: (IconAppearance) -> Unit,
    onIconEditorOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemUi = rememberSystemUiController()
    SideEffect {
        systemUi.setSystemBarsColor(Color.Transparent, darkIcons = false)
    }

    var actionApp by remember { mutableStateOf<AppInfo?>(null) }
    var iconDraft by remember { mutableStateOf(IconAppearance.Default) }

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

    CompositionLocalProvider(LocalIconAppearance provides appearance) {
        Box(modifier = modifier.fillMaxSize()) {
            HomeScreen(
                state = state,
                onLaunchApp = onLaunchApp,
                onAppLongClick = { actionApp = it },
                onOpenDrawer = { onDrawerOpenChange(true) },
                onOpenDrawerSearch = onOpenDrawerSearch,
                onEditModeChange = onEditModeChange,
                onRemoveHomeItem = onRemoveHomeItem,
                onSwapHomeItems = onSwapHomeItems,
                onCreateFolder = onCreateFolder,
                onAddAppToFolder = onAddAppToFolder,
                onAddPage = onAddPage,
                onAddAppToHome = { app, _ -> onAddAppToHome(app) },
                modifier = Modifier.fillMaxSize()
            )

            AppDrawer(
                visible = state.isDrawerOpen,
                state = state,
                onLaunchApp = onLaunchApp,
                onAppLongClick = { actionApp = it },
                onAddAppToHome = onAddAppToHome,
                onSearchQueryChange = onSearchQueryChange,
                onClose = { onDrawerOpenChange(false) },
                modifier = Modifier.fillMaxSize()
            )

            AppActionsSheet(
                app = actionApp,
                onDismiss = { actionApp = null },
                onFavorite = onToggleFavorite,
                onAddToHome = onAddAppToHome,
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
