package com.voidlauncher.app.ui.shade

import android.Manifest
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.voidlauncher.app.notifications.NotificationMirror
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.WallpaperHazeSource
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CcSmoothing = 0.78f
private val CcRed = Color(0xFFFF3B30)
private val CcStretchSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow
)

/** iOS-style rubber band: extra drag maps to a diminishing offset. */
internal fun ccRubberBand(overscroll: Float, limit: Float = 160f): Float {
    if (overscroll <= 0f) return 0f
    return (overscroll * limit) / (overscroll + limit)
}

private enum class CcExpandedModule { Wifi, Bluetooth, Mobile }

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
    expansion: Float,
    controller: ControlCenterController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val shown = visible || expansion > 0.001f
    val interactive = expansion >= 0.98f
    var expanded by remember { mutableStateOf<CcExpandedModule?>(null) }
    var lastExpanded by remember { mutableStateOf<CcExpandedModule?>(null) }
    LaunchedEffect(expanded) {
        if (expanded != null) lastExpanded = expanded
    }
    BackHandler(enabled = expanded != null) { expanded = null }
    BackHandler(enabled = shown && expansion > 0.45f && expanded == null, onBack = onClose)
    val pull = remember { Animatable(0f) }

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

    DisposableEffect(shown) {
        if (shown) controller.startListening()
        onDispose { controller.stopListening() }
    }

    LaunchedEffect(shown) {
        if (shown) {
            controller.refresh()
            pull.snapTo(0f)
            expanded = null
            while (true) {
                delay(900)
                controller.refresh()
            }
        }
    }

    if (!shown) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (pull.value < -120f) {
                                        onClose()
                                    }
                                    scope.launch { pull.animateTo(0f, CcStretchSpring) }
                                },
                                onDragCancel = {
                                    scope.launch { pull.animateTo(0f, CcStretchSpring) }
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    scope.launch { pull.snapTo(pull.value + amount) }
                                }
                            )
                        }
                } else {
                    Modifier
                }
            )
    ) {
        WallpaperHazeSource(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = expansion.coerceIn(0f, 1f) }
                .blur(42.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f * expansion.coerceIn(0f, 1f)))
        )
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.74f)
                .statusBarsPadding()
                .padding(top = 76.dp)
                .graphicsLayer {
                    val over = (expansion - 1f).coerceAtLeast(0f)
                    val stretch = ccRubberBand(over * size.height) +
                        if (pull.value > 0f) ccRubberBand(pull.value) else 0f
                    val dragUp = if (pull.value < 0f) pull.value else 0f
                    translationY =
                        (1f - expansion.coerceIn(0f, 1f)) * -size.height + stretch + dragUp
                    val h = size.height.coerceAtLeast(1f)
                    val down = stretch / h
                    val up = if (dragUp < 0f) (-dragUp) / h else 0f
                    // Pull down: taller + narrower. Pull up: shorter + wider. Content scales with the panel.
                    scaleX = (1f - down * 0.14f + up * 0.16f).coerceIn(0.82f, 1.22f)
                    scaleY = (1f + down * 0.32f - up * 0.18f).coerceIn(0.78f, 1.4f)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    alpha = (expansion * 1.15f).coerceIn(0f, 1f)
                }
                .then(
                    if (interactive) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
                val gap = 14.dp
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
                        onToggle = { controller.toggleWifi() },
                        onOpen = { controller.openWifi() },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            controller.requestWifiScan()
                            expanded = CcExpandedModule.Wifi
                        },
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
                        onToggle = {
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
                        onOpen = { controller.openBluetooth() },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            expanded = CcExpandedModule.Bluetooth
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
                        onToggle = {
                            controller.toggleMobileData {
                                requestPhone.launch(Manifest.permission.READ_PHONE_STATE)
                            }
                        },
                        onOpen = { controller.openNetwork() },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            expanded = CcExpandedModule.Mobile
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

            AnimatedVisibility(
                visible = expanded != null,
                enter = fadeIn() + scaleIn(initialScale = 0.86f, transformOrigin = TransformOrigin.Center),
                exit = fadeOut() + scaleOut(targetScale = 0.92f, transformOrigin = TransformOrigin.Center),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expanded = null }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val module = expanded ?: lastExpanded
                    if (module != null) {
                        ExpandedModuleCard(
                            module = module,
                            controller = controller,
                            onToggleWifi = { controller.toggleWifi() },
                            onToggleBluetooth = {
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
                            onToggleMobile = {
                                controller.toggleMobileData {
                                    requestPhone.launch(Manifest.permission.READ_PHONE_STATE)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
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
        enableSheen = false,
        enableRefraction = false,
        blurStrengthOverride = 1.15f,
        liquidStroke = true,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectivityPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CcGlass(
        modifier = modifier,
        capsule = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (active) IosBlue else Color.White.copy(alpha = 0.16f))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                        onLongClick = onLongPress
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpen,
                        onLongClick = onLongPress
                    )
            ) {
                Text(
                    text = title,
                    color = VoidMist,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = VoidMuted,
                    fontSize = 12.sp,
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
        capsule = true,
        activeTint = if (active) Color.White else Color.Transparent,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) CcRed else Color.White,
                modifier = Modifier.size(28.dp)
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
                    .background(Color.White)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun ExpandedModuleCard(
    module: CcExpandedModule,
    controller: ControlCenterController,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleMobile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (module) {
        CcExpandedModule.Wifi -> Icons.Rounded.Wifi
        CcExpandedModule.Bluetooth -> Icons.Rounded.Bluetooth
        CcExpandedModule.Mobile -> Icons.Rounded.SignalCellularAlt
    }
    val title = when (module) {
        CcExpandedModule.Wifi -> "Wi‑Fi"
        CcExpandedModule.Bluetooth -> "Bluetooth"
        CcExpandedModule.Mobile -> "Mobile Data"
    }
    val active = when (module) {
        CcExpandedModule.Wifi -> controller.wifiEnabled
        CcExpandedModule.Bluetooth -> controller.bluetoothEnabled
        CcExpandedModule.Mobile -> controller.mobileDataEnabled
    }
    val subtitle = when (module) {
        CcExpandedModule.Wifi -> when {
            !controller.wifiEnabled -> "Off"
            controller.wifiSsid.isNotBlank() -> controller.wifiSsid
            else -> "Not Connected"
        }
        CcExpandedModule.Bluetooth -> when {
            !controller.bluetoothEnabled -> "Off"
            controller.bluetoothName.isNotBlank() -> controller.bluetoothName
            else -> "On"
        }
        CcExpandedModule.Mobile -> when {
            !controller.mobileDataEnabled -> "Off"
            controller.mobileCarrier.isNotBlank() -> controller.mobileCarrier
            else -> "On"
        }
    }
    val rows = when (module) {
        CcExpandedModule.Wifi -> controller.nearbyWifiNames()
        CcExpandedModule.Bluetooth -> controller.pairedBluetoothNames()
        CcExpandedModule.Mobile -> listOfNotNull(
            controller.mobileCarrier.takeIf { it.isNotBlank() }
        )
    }
    val settingsLabel = when (module) {
        CcExpandedModule.Wifi -> "Wi‑Fi Settings"
        CcExpandedModule.Bluetooth -> "Bluetooth Settings"
        CcExpandedModule.Mobile -> "Cellular Settings"
    }
    val onToggle = when (module) {
        CcExpandedModule.Wifi -> onToggleWifi
        CcExpandedModule.Bluetooth -> onToggleBluetooth
        CcExpandedModule.Mobile -> onToggleMobile
    }
    val onSettings = when (module) {
        CcExpandedModule.Wifi -> controller::openWifi
        CcExpandedModule.Bluetooth -> controller::openBluetooth
        CcExpandedModule.Mobile -> controller::openNetwork
    }

    CcGlass(
        modifier = modifier.heightIn(min = 220.dp),
        capsule = false,
        cornerRadius = 36.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (active) IosBlue else Color.White.copy(alpha = 0.16f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = VoidMist,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = VoidMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (rows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                rows.forEachIndexed { index, name ->
                    val selected = when (module) {
                        CcExpandedModule.Wifi -> name == controller.wifiSsid
                        else -> index == 0 && active
                    }
                    Text(
                        text = name,
                        color = if (selected) IosBlue else VoidMist,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (module) {
                        CcExpandedModule.Wifi -> "No networks to show"
                        CcExpandedModule.Bluetooth -> "No paired devices"
                        CcExpandedModule.Mobile -> "Carrier unavailable"
                    },
                    color = VoidMuted,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = settingsLabel,
                color = IosBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettings
                    )
                    .padding(vertical = 8.dp)
            )
        }
    }
}
