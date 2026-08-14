package com.voidlauncher.app.ui.shade

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.app.notifications.NotificationMirror
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.IosBlueGlass
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.delay

private const val CcSmoothing = 1f

/**
 * 4×4 Control Center:
 *  [ Music 2×2 ] [ Wi‑Fi 1×2 ]
 *                [ BT    1×2 ]
 *  [ Rot 1×1 ] [ Silent 1×1 ] [ Bright 2×1 ] [ Vol 2×1 ]
 *  [ Mobile Data 1×2        ]
 */
@Composable
fun ControlCenter(
    visible: Boolean,
    controller: ControlCenterController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(enabled = visible, onBack = onClose)
    var pull by remember { mutableFloatStateOf(0f) }

    val requestBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        controller.refresh()
        if (result.values.any { it }) {
            controller.toggleBluetooth { }
        }
    }
    val requestPhone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { controller.refresh() }

    DisposableEffect(visible) {
        if (visible) controller.startListening()
        onDispose { controller.stopListening() }
    }

    LaunchedEffect(visible) {
        if (visible) {
            controller.refresh()
            pull = 0f
            while (true) {
                delay(900)
                controller.refresh()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(340, easing = FastOutSlowInEasing)) { -it / 6 },
        exit = fadeOut(tween(160)) +
            slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 8 },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
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
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.84f)
                    .statusBarsPadding()
                    .padding(top = 84.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                val gap = 8.dp
                val cell = (maxWidth - gap * 3) / 4
                val gridH = cell * 4 + gap * 3

                fun Modifier.place(col: Int, row: Int, colSpan: Int = 1, rowSpan: Int = 1): Modifier {
                    val w = cell * colSpan + gap * (colSpan - 1)
                    val h = cell * rowSpan + gap * (rowSpan - 1)
                    return offset(x = (cell + gap) * col, y = (cell + gap) * row)
                        .width(w)
                        .height(h)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridH)
                ) {
                    MusicTile(
                        playing = controller.nowPlaying,
                        onPlayPause = { controller.playPause() },
                        onNext = { controller.skipNext() },
                        onPrev = { controller.skipPrevious() },
                        onOpen = {
                            if (controller.nowPlaying.hasSession) {
                                controller.openNowPlayingApp()
                            } else {
                                NotificationMirror.openAccessSettings(context)
                            }
                        },
                        modifier = Modifier.place(0, 0, 2, 2)
                    )
                    ConnectivityPill(
                        icon = Icons.Rounded.Wifi,
                        title = "Wi‑Fi",
                        subtitle = when {
                            !controller.wifiEnabled -> "Off"
                            controller.wifiSsid.isNotBlank() -> controller.wifiSsid
                            else -> "On"
                        },
                        active = controller.wifiEnabled,
                        onClick = { controller.toggleWifi() },
                        modifier = Modifier.place(2, 0, 2, 1)
                    )
                    ConnectivityPill(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth",
                        subtitle = when {
                            !controller.bluetoothEnabled -> "Off"
                            controller.bluetoothName.isNotBlank() -> controller.bluetoothName
                            else -> "On"
                        },
                        active = controller.bluetoothEnabled,
                        onClick = {
                            controller.toggleBluetooth {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    requestBluetooth.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.place(2, 1, 2, 1)
                    )
                    IconTile(
                        icon = if (controller.rotationLocked) {
                            Icons.Rounded.ScreenLockRotation
                        } else {
                            Icons.Rounded.ScreenRotation
                        },
                        active = controller.rotationLocked,
                        onClick = { controller.toggleRotationLock() },
                        modifier = Modifier.place(0, 2, 1, 1)
                    )
                    IconTile(
                        icon = if (controller.dndOn) {
                            Icons.Rounded.NotificationsOff
                        } else {
                            Icons.Rounded.NotificationsActive
                        },
                        active = controller.dndOn,
                        onClick = { controller.toggleDnd() },
                        modifier = Modifier.place(1, 2, 1, 1)
                    )
                    ConnectivityPill(
                        icon = Icons.Rounded.SignalCellularAlt,
                        title = "Mobile Data",
                        subtitle = when {
                            !controller.mobileDataEnabled -> "Off"
                            controller.mobileCarrier.isNotBlank() -> controller.mobileCarrier
                            else -> "On"
                        },
                        active = controller.mobileDataEnabled,
                        onClick = {
                            controller.toggleMobileData {
                                requestPhone.launch(Manifest.permission.READ_PHONE_STATE)
                            }
                        },
                        modifier = Modifier.place(0, 3, 2, 1)
                    )
                    VerticalLevelSlider(
                        value = controller.brightness,
                        onValueChange = { controller.applyBrightness(it) },
                        icon = if (controller.brightness < 0.45f) {
                            Icons.Rounded.BrightnessLow
                        } else {
                            Icons.Rounded.BrightnessHigh
                        },
                        modifier = Modifier.place(2, 2, 1, 2)
                    )
                    VerticalLevelSlider(
                        value = controller.volume,
                        onValueChange = { controller.applyVolume(it) },
                        icon = when {
                            controller.volume <= 0.01f -> Icons.Rounded.VolumeOff
                            controller.volume < 0.5f -> Icons.Rounded.VolumeDown
                            else -> Icons.Rounded.VolumeUp
                        },
                        modifier = Modifier.place(3, 2, 1, 2)
                    )
                }
            }
        }
    }
}

@Composable
private fun CcGlass(
    modifier: Modifier,
    capsule: Boolean,
    cornerRadius: Dp = 28.dp,
    activeTint: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassPanel(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        ),
        cornerRadius = cornerRadius,
        strong = true,
        enableSheen = true,
        enableRefraction = true,
        cornerSmoothing = CcSmoothing,
        capsule = capsule,
        tint = activeTint,
        content = content
    )
}

@Composable
private fun MusicTile(
    playing: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    CcGlass(
        modifier = modifier,
        capsule = false,
        cornerRadius = 32.dp,
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val art = playing.artwork
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = VoidMist,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (playing.hasSession && playing.title.isNotBlank()) {
                            playing.title
                        } else {
                            "Not Playing"
                        },
                        color = VoidMist,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (playing.hasSession && playing.artist.isNotBlank()) {
                            playing.artist
                        } else {
                            "Music"
                        },
                        color = VoidMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = VoidMist,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPrev
                        )
                )
                Icon(
                    if (playing.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = VoidMist,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayPause
                        )
                )
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = VoidMist,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNext
                        )
                )
            }
        }
    }
}

@Composable
private fun ConnectivityPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CcGlass(
        modifier = modifier,
        capsule = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (active) IosBlue else Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = VoidMist,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = VoidMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IconTile(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CcGlass(
        modifier = modifier,
        capsule = false,
        cornerRadius = 28.dp,
        activeTint = if (active) IosBlueGlass else Color.Transparent,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun VerticalLevelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var local by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { local = value }

    CcGlass(
        modifier = modifier,
        capsule = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        local = (local - dragAmount / size.height).coerceIn(0f, 1f)
                        onValueChange(local)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(local.coerceIn(0.04f, 1f))
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.38f))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(22.dp)
            )
        }
    }
}
