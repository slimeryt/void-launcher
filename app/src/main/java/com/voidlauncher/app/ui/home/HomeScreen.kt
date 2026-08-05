package com.voidlauncher.app.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.SettingsActivity
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.components.DockBar
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.HomeClock
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState

@Composable
fun HomeScreen(
    state: LauncherUiState,
    onLaunchApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var editMode by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Empty-home gestures (under UI chrome)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(editMode) {
                    detectTapGestures(
                        onLongPress = { editMode = true },
                        onTap = { if (editMode) editMode = false }
                    )
                }
                .pointerInput(editMode) {
                    if (editMode) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragAccum < -80f) onOpenDrawer()
                            dragAccum = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragAccum += dragAmount
                        }
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            HomeClock()

            AnimatedVisibility(visible = editMode, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "Edit Home",
                    style = MaterialTheme.typography.titleMedium,
                    color = VoidCyan,
                    modifier = Modifier.padding(start = 28.dp, top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(visible = !editMode, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassPanel(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp),
                            cornerRadius = 99.dp,
                            enableSheen = false,
                            enableRefraction = true
                        ) {}
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            AnimatedVisibility(visible = editMode, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    context.startActivity(
                                        Intent(context, SettingsActivity::class.java)
                                    )
                                    editMode = false
                                }
                            },
                        cornerRadius = 28.dp,
                        strong = true,
                        enableSheen = true,
                        enableRefraction = true
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = VoidMist,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap empty space to finish",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoidMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            DockBar(
                apps = state.dockApps,
                iconScale = state.iconScale,
                onAppClick = { if (!editMode) onLaunchApp(it) },
                onAppLongClick = { if (!editMode) onAppLongClick(it) },
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .alpha(if (editMode) 0.45f else 1f)
            )
        }
    }
}
