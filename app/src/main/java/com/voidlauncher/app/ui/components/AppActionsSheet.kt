package com.voidlauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.theme.VoidMist
import kotlin.math.roundToInt

private val SheetShape = SmoothCornerShape(32.dp)
private val MenuBlack = Color(0xFF1C1C1E)

/**
 * Compact floating context menu near the focused icon.
 *
 * While [outsideDismissEnabled] is false (finger still down after long-press), only the
 * menu column is composed — no full-screen scrim — so the same hold finger can drag.
 */
@Composable
fun AppActionsSheet(
    app: AppInfo?,
    anchorBounds: Rect? = null,
    outsideDismissEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onFavorite: (AppInfo) -> Unit,
    onHide: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit
) {
    if (app == null) return

    val density = LocalDensity.current
    val screenW = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenH = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    fun menuOffset(): IntOffset {
        val menuW = with(density) { 220.dp.toPx() }
        if (anchorBounds == null || anchorBounds.width <= 1f) {
            return IntOffset(
                ((screenW - menuW) / 2f).roundToInt(),
                (screenH / 3f).roundToInt()
            )
        }
        val menuH = with(density) { 200.dp.toPx() }
        val gap = with(density) { 12.dp.toPx() }
        var x = anchorBounds.center.x - menuW / 2f
        x = x.coerceIn(16f, (screenW - menuW - 16f).coerceAtLeast(16f))
        var y = anchorBounds.bottom + gap
        if (y + menuH > screenH - 24f) {
            y = (anchorBounds.top - menuH - gap).coerceAtLeast(24f)
        }
        return IntOffset(x.roundToInt(), y.roundToInt())
    }

    @Composable
    fun MenuBody(interactive: Boolean) {
        Column(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 240.dp)
                .width(220.dp)
                .shadow(28.dp, SheetShape, clip = false)
                .clip(SheetShape)
                .background(MenuBlack)
                .then(
                    if (interactive) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* absorb */ }
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = app.label,
                color = VoidMist.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (interactive) {
                CompactAction("Favorite") {
                    onFavorite(app)
                    onDismiss()
                }
                CompactAction("Hide") {
                    onHide(app)
                    onDismiss()
                }
                CompactAction("App Info") {
                    onAppInfo(app)
                    onDismiss()
                }
            } else {
                CompactActionLabel("Favorite")
                CompactActionLabel("Hide")
                CompactActionLabel("App Info")
            }
        }
    }

    if (outsideDismissEnabled) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { menuOffset() }
            ) {
                MenuBody(interactive = true)
            }
        }
    } else {
        // Sized only to the menu — does not cover home or cancel the hold pointer.
        Box(
            modifier = Modifier
                .offset { menuOffset() }
        ) {
            MenuBody(interactive = false)
        }
    }
}

@Composable
private fun CompactAction(
    label: String,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        color = VoidMist,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    )
}

@Composable
private fun CompactActionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        color = VoidMist,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp)
    )
}
