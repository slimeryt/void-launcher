package com.voidlauncher.app.ui.shade

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.voidlauncher.app.ui.statusbar.polarStatusPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.voidlauncher.app.notifications.NotificationMirror
import com.voidlauncher.app.notifications.ShadeNotification
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * iOS-style Notification Center: large lock-screen clock/date, stacked
 * frosted cards, clear control — no settings-page chrome.
 */
@Composable
fun NotificationCenter(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onClose)
    val context = LocalContext.current
    val items by NotificationMirror.items.collectAsState()
    var accessGranted by remember { mutableStateOf(NotificationMirror.isAccessGranted(context)) }
    var pull by remember { mutableFloatStateOf(0f) }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(visible) {
        if (visible) {
            accessGranted = NotificationMirror.isAccessGranted(context)
            pull = 0f
            while (true) {
                now = Date()
                delay(1_000)
            }
        }
    }

    val time = remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    }
    val date = remember(now) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(340, easing = FastOutSlowInEasing)) { -it / 5 },
        exit = fadeOut(tween(160)) +
            slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 6 },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.48f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (pull < -100f) onClose()
                            pull = 0f
                        },
                        onDragCancel = { pull = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            if (amount < 0f) pull += amount
                            else pull = (pull + amount).coerceAtMost(0f)
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .polarStatusPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Lock-screen style header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date,
                        color = VoidMist.copy(alpha = 0.9f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = time,
                        color = VoidMist,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-1.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                when {
                    !accessGranted -> {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            cornerRadius = 24.dp,
                            strong = true,
                            enableSheen = true,
                            enableRefraction = true
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = VoidMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Notification Access",
                                    color = VoidMist,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                Text(
                                    text = "Allow Polar to show alerts here. If Android says “denied”, open App info → ⋮ → Allow restricted settings first.",
                                    color = VoidMuted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .clickable {
                                            NotificationMirror.openAccessSettings(context)
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Open Settings",
                                        color = VoidMist,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    items.isEmpty() -> {
                        Text(
                            text = "No Notifications",
                            color = VoidMist.copy(alpha = 0.55f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 48.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items, key = { it.key }) { item ->
                                NotificationCard(
                                    item = item,
                                    onClick = {
                                        runCatching { item.contentIntent?.send() }
                                        onClose()
                                    },
                                    onClear = {
                                        if (item.isClearable) {
                                            NotificationMirror.cancel(item.key)
                                        }
                                    }
                                )
                            }
                        }

                        if (items.any { it.isClearable }) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable { NotificationMirror.cancelAll() }
                                    .padding(horizontal = 22.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    color = VoidMist,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: ShadeNotification,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val time = remember(item.postTime) {
        DateUtils.getRelativeTimeSpanString(
            item.postTime,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val bitmap = remember(item.key) {
        item.icon?.let { runCatching { it.toBitmap(96, 96) }.getOrNull() }
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 22.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Text(
                        text = item.appLabel.take(1).uppercase(),
                        color = VoidMist,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.appLabel.uppercase(Locale.getDefault()),
                        color = VoidMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = time,
                        color = VoidMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = item.title,
                    color = VoidMist,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        color = VoidMist.copy(alpha = 0.78f),
                        fontSize = 14.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (item.isClearable) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = VoidMist,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
