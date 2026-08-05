package com.voidlauncher.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.AppActionsSheet
import com.voidlauncher.app.ui.drawer.AppDrawer
import com.voidlauncher.app.ui.home.HomeScreen
import com.voidlauncher.app.viewmodel.LauncherUiState

@Composable
fun LauncherRoot(
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDrawerOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemUi = rememberSystemUiController()
    SideEffect {
        systemUi.setSystemBarsColor(Color.Transparent, darkIcons = false)
    }

    var actionApp by remember { mutableStateOf<AppInfo?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreen(
            state = state,
            onLaunchApp = onLaunchApp,
            onAppLongClick = { actionApp = it },
            onOpenDrawer = { onDrawerOpenChange(true) },
            modifier = Modifier.fillMaxSize()
        )

        AppDrawer(
            visible = state.isDrawerOpen,
            state = state,
            onLaunchApp = onLaunchApp,
            onAppLongClick = { actionApp = it },
            onSearchQueryChange = onSearchQueryChange,
            onClose = { onDrawerOpenChange(false) },
            modifier = Modifier.fillMaxSize()
        )

        AppActionsSheet(
            app = actionApp,
            onDismiss = { actionApp = null },
            onFavorite = onToggleFavorite,
            onHide = onHideApp,
            onAppInfo = onAppInfo
        )
    }
}
