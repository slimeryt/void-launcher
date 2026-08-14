package com.voidlauncher.app.ui.shade

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.IosBlueGlass

/**
 * iOS Control Center grid: one horizontal connectivity plate + two tall
 * vertical sliders, then a row of circular glass shortcuts.
 */
@Composable
fun ControlCenter(
    visible: Boolean,
    controller: ControlCenterController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onClose)
    var pull by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            controller.refresh()
            pull = 0f
        }
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
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f)
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
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // iOS cluster: 2-wide connectivity + 2 tall sliders (not a 2×2 of squares)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ConnectivityModule(
                        modifier = Modifier.weight(2f),
                        onAirplane = {
                            controller.openAirplane()
                            onClose()
                        },
                        onCellular = {
                            controller.openNetwork()
                            onClose()
                        },
                        onWifi = {
                            controller.openWifi()
                            onClose()
                        },
                        onBluetooth = {
                            controller.openBluetooth()
                            onClose()
                        }
                    )
                    VerticalLevelSlider(
                        value = controller.brightness,
                        onValueChange = { controller.applyBrightness(it) },
                        icon = if (controller.brightness < 0.45f) {
                            Icons.Rounded.BrightnessLow
                        } else {
                            Icons.Rounded.BrightnessHigh
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VerticalLevelSlider(
                        value = controller.volume,
                        onValueChange = { controller.applyVolume(it) },
                        icon = when {
                            controller.volume <= 0.01f -> Icons.Rounded.VolumeOff
                            controller.volume < 0.5f -> Icons.Rounded.VolumeDown
                            else -> Icons.Rounded.VolumeUp
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CcRoundToggle(
                        icon = if (controller.flashlightOn) {
                            Icons.Rounded.FlashlightOn
                        } else {
                            Icons.Rounded.FlashlightOff
                        },
                        active = controller.flashlightOn,
                        activeTint = Color(0xFFFFCC00).copy(alpha = 0.55f),
                        onClick = { controller.toggleFlashlight() }
                    )
                    CcRoundToggle(
                        icon = if (controller.rotationLocked) {
                            Icons.Rounded.ScreenLockRotation
                        } else {
                            Icons.Rounded.ScreenRotation
                        },
                        active = controller.rotationLocked,
                        onClick = { controller.toggleRotationLock() }
                    )
                    val ringIcon = when (controller.ringMode) {
                        RingMode.Normal -> Icons.Rounded.VolumeUp
                        RingMode.Vibrate -> Icons.Rounded.Vibration
                        RingMode.Silent -> Icons.Rounded.VolumeOff
                    }
                    CcRoundToggle(
                        icon = ringIcon,
                        active = controller.ringMode != RingMode.Normal,
                        onClick = { controller.cycleRingMode() }
                    )
                    CcRoundToggle(
                        icon = Icons.Rounded.BatterySaver,
                        active = false,
                        onClick = {
                            controller.openBattery()
                            onClose()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectivityModule(
    onAirplane: () -> Unit,
    onCellular: () -> Unit,
    onWifi: () -> Unit,
    onBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.aspectRatio(1f),
        cornerRadius = 36.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CcIconButton(
                    icon = Icons.Rounded.AirplanemodeActive,
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = onAirplane
                )
                CcIconButton(
                    icon = Icons.Rounded.SignalCellularAlt,
                    active = false,
                    modifier = Modifier.weight(1f),
                    onClick = onCellular
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CcIconButton(
                    icon = Icons.Rounded.Wifi,
                    active = true,
                    activeColor = IosBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onWifi
                )
                CcIconButton(
                    icon = Icons.Rounded.Bluetooth,
                    active = true,
                    activeColor = IosBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onBluetooth
                )
            }
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

    // width:height = 1:2 so these stay tall capsules next to the square cluster
    GlassPanel(
        modifier = modifier.aspectRatio(0.5f),
        cornerRadius = 36.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = true
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
                    .padding(bottom = 18.dp)
                    .size(26.dp)
            )
        }
    }
}

@Composable
private fun CcIconButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = IosBlue
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(if (active) activeColor else Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CcRoundToggle(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    activeTint: Color = IosBlueGlass
) {
    GlassPanel(
        modifier = Modifier
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 32.dp,
        strong = true,
        enableSheen = true,
        enableRefraction = true,
        tint = if (active) activeTint else Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
