package com.voidlauncher.app.ui.statusbar

import android.app.Activity
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularAlt1Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt2Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Wifi1Bar
import androidx.compose.material.icons.rounded.Wifi2Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.VoidBody
import kotlinx.coroutines.delay
import java.util.Date

private val BatteryEmpty = Color.White.copy(alpha = 0.35f)
private val BatteryIdleFill = Color(0xFFF5F5F7)
private val BatteryChargingFill = Color(0xFF30D158)
private val BatteryHoldFill = Color(0xFF8E8E93)
private val BatteryLowPowerFill = Color(0xFFFFD60A)
private val BatteryCriticalFill = Color(0xFFFF3B30)
private val StatusGlyph = Color(0xFFF5F5F7)

/** iOS 27 body: rounded rectangle, not a stadium / circle. */
private val BatteryBodyShape = SmoothCornerShape(3.2.dp)

/** Keep content out of the cutout even after the system status bar is hidden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.polarStatusPadding(): Modifier =
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)

@Composable
fun HideSystemStatusBar() {
    val view = LocalView.current
    val window = (LocalContext.current as? Activity)?.window ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(view, window, lifecycleOwner) {
        val controller = WindowCompat.getInsetsController(window, view)
        fun hide() {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        hide()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hide()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PolarStatusBar(
    controller: PolarStatusBarController,
    shadeLocked: Boolean,
    onPullNotificationCenter: () -> Unit,
    onPullControlCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val time = remember(now, pattern) { DateFormat.format(pattern, now).toString() }

    val density = LocalDensity.current
    val insetTop = WindowInsets.statusBarsIgnoringVisibility.getTop(density)
    val heightDp = with(density) { insetTop.toDp() }.coerceAtLeast(24.dp)

    var pull by remember { mutableFloatStateOf(0f) }
    var startX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.displayCutout.union(WindowInsets.statusBarsIgnoringVisibility)
                    .only(WindowInsetsSides.Horizontal)
            )
            .height(heightDp)
            .pointerInput(shadeLocked) {
                if (shadeLocked) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startX = offset.x
                        pull = 0f
                    },
                    onDragEnd = {
                        if (pull > 36f) {
                            if (startX < size.width * 0.5f) onPullNotificationCenter()
                            else onPullControlCenter()
                        }
                        pull = 0f
                    },
                    onDragCancel = { pull = 0f },
                    onVerticalDrag = { _, amount ->
                        pull = (pull + amount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 22.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                color = StatusGlyph,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = VoidBody,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (controller.airplane) {
                    StatusCcIcon(Icons.Rounded.AirplanemodeActive)
                } else {
                    StatusCcIcon(
                        when (controller.cellularBars) {
                            0, 1 -> Icons.Rounded.SignalCellularAlt1Bar
                            2 -> Icons.Rounded.SignalCellularAlt2Bar
                            else -> Icons.Rounded.SignalCellularAlt
                        }
                    )
                }
                if (controller.wifiConnected) {
                    StatusCcIcon(
                        when (controller.wifiBars) {
                            0, 1 -> Icons.Rounded.Wifi1Bar
                            2 -> Icons.Rounded.Wifi2Bar
                            else -> Icons.Rounded.Wifi
                        }
                    )
                }
                Ios27BatteryIcon(
                    percent = controller.batteryPercent,
                    glyph = controller.batteryGlyph
                )
            }
        }
    }
}

@Composable
private fun StatusCcIcon(image: ImageVector) {
    Icon(
        imageVector = image,
        contentDescription = null,
        tint = StatusGlyph,
        modifier = Modifier.size(15.dp)
    )
}

/**
 * iOS 27 status-bar battery: borderless **rounded rectangle** + rectangular
 * terminal, fill to remaining charge, percentage inside. Not a pill / circle.
 */
@Composable
fun Ios27BatteryIcon(
    percent: Int,
    glyph: BatteryGlyph,
    modifier: Modifier = Modifier
) {
    val value = percent.coerceIn(0, 100)
    val solid = glyph != BatteryGlyph.Idle
    val track = when (glyph) {
        BatteryGlyph.Idle -> BatteryIdleFill
        BatteryGlyph.Charging -> BatteryChargingFill
        BatteryGlyph.Hold -> BatteryHoldFill
        BatteryGlyph.LowPower -> BatteryLowPowerFill
        BatteryGlyph.Critical -> BatteryCriticalFill
    }
    val onFill = when (glyph) {
        BatteryGlyph.Idle, BatteryGlyph.LowPower -> Color(0xFF1C1C1E)
        else -> Color.White
    }
    val fillFrac by animateFloatAsState(
        targetValue = if (solid) 1f else value / 100f,
        animationSpec = tween(220),
        label = "batteryFill"
    )
    val bodyW = 24.dp
    val bodyH = 13.dp

    Row(
        modifier = modifier.height(bodyH),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(bodyW)
                .fillMaxHeight()
                .clip(BatteryBodyShape)
                .background(BatteryEmpty),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFrac.coerceIn(0f, 1f))
                    .align(Alignment.CenterStart)
                    .background(track)
            )
            Text(
                text = value.toString(),
                color = onFill,
                fontSize = if (value >= 100) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = VoidBody,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(0.9.dp))
        Box(
            modifier = Modifier
                .width(1.6.dp)
                .height(5.2.dp)
                .clip(RoundedCornerShape(0.7.dp))
                .background(if (fillFrac >= 0.97f) track else BatteryEmpty)
        )
    }
}
