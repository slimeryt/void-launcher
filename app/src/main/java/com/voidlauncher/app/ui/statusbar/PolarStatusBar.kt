package com.voidlauncher.app.ui.statusbar

import android.app.Activity
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.voidlauncher.app.ui.theme.VoidBody
import kotlinx.coroutines.delay
import java.util.Date

private val BatteryIdleFill = Color(0xFFE5E5EA)
private val BatteryChargingFill = Color(0xFF30D158)
private val BatteryHoldFill = Color(0xFF8E8E93)
private val BatteryLowPowerFill = Color(0xFFFFD60A)
private val BatteryCriticalFill = Color(0xFFFF3B30)
private val StatusGlyph = Color(0xFFF5F5F7)
private val StatusGlyphDim = Color(0x59F5F5F7)

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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (controller.airplane) {
                    AirplaneGlyph(Modifier.size(15.dp, 13.dp))
                } else {
                    CellularGlyph(
                        bars = controller.cellularBars,
                        modifier = Modifier.size(17.dp, 12.dp)
                    )
                }
                if (controller.wifiConnected) {
                    WifiGlyph(
                        bars = controller.wifiBars,
                        modifier = Modifier.size(16.dp, 12.dp)
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
fun Ios27BatteryIcon(
    percent: Int,
    glyph: BatteryGlyph,
    modifier: Modifier = Modifier
) {
    val value = percent.coerceIn(0, 100)
    val fill = when (glyph) {
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
    val bodyW = 27.dp
    val bodyH = 13.dp
    val nubW = 1.5.dp
    val gap = 0.8.dp
    Box(
        modifier = modifier.size(bodyW + gap + nubW, bodyH),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.matchParentSize()) {
            val h = size.height
            val bodyWidth = h * (27f / 13f)
            val radius = h / 2f
            val nubWidth = h * (1.5f / 13f)
            val nubHeight = h * (4.6f / 13f)
            val gapPx = h * (0.8f / 13f)
            drawRoundRect(
                color = fill,
                topLeft = Offset.Zero,
                size = Size(bodyWidth, h),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = fill,
                topLeft = Offset(bodyWidth + gapPx, (h - nubHeight) / 2f),
                size = Size(nubWidth, nubHeight),
                cornerRadius = CornerRadius(nubWidth, nubWidth)
            )
        }
        Text(
            text = value.toString(),
            color = onFill,
            fontSize = if (value >= 100) 9.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = VoidBody,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(bodyW)
                .align(Alignment.CenterStart)
                .padding(end = gap),
            maxLines = 1
        )
    }
}

@Composable
private fun CellularGlyph(bars: Int, modifier: Modifier = Modifier) {
    val lit = bars.coerceIn(0, 4)
    Canvas(modifier) {
        val gap = size.width * 0.14f
        val barW = (size.width - gap * 3f) / 4f
        val radii = CornerRadius(barW / 2f, barW / 2f)
        val fractions = floatArrayOf(0.34f, 0.54f, 0.74f, 1f)
        for (i in 0..3) {
            val h = size.height * fractions[i]
            drawRoundRect(
                color = if (i < lit) StatusGlyph else StatusGlyphDim,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = radii
            )
        }
    }
}

@Composable
private fun WifiGlyph(bars: Int, modifier: Modifier = Modifier) {
    val lit = bars.coerceIn(0, 3)
    Canvas(modifier) {
        val stroke = Stroke(
            width = size.minDimension * 0.14f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val cx = size.width / 2f
        val cy = size.height * 0.92f
        val radii = floatArrayOf(size.minDimension * 0.28f, size.minDimension * 0.55f, size.minDimension * 0.82f)
        for (i in 0..2) {
            val path = Path().apply {
                val r = radii[i]
                addArc(
                    Rect(cx - r, cy - r, cx + r, cy + r),
                    startAngleDegrees = 220f,
                    sweepAngleDegrees = 100f
                )
            }
            drawPath(
                path = path,
                color = if (i < lit) StatusGlyph else StatusGlyphDim,
                style = stroke
            )
        }
        drawCircle(
            color = if (lit > 0) StatusGlyph else StatusGlyphDim,
            radius = stroke.width * 0.42f,
            center = Offset(cx, cy - size.minDimension * 0.04f)
        )
    }
}

@Composable
private fun AirplaneGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.62f)
            lineTo(size.width * 0.42f, size.height * 0.48f)
            lineTo(size.width * 0.22f, size.height * 0.18f)
            lineTo(size.width * 0.38f, size.height * 0.22f)
            lineTo(size.width * 0.58f, size.height * 0.42f)
            lineTo(size.width * 0.92f, size.height * 0.28f)
            lineTo(size.width * 0.62f, size.height * 0.58f)
            lineTo(size.width * 0.78f, size.height * 0.82f)
            lineTo(size.width * 0.64f, size.height * 0.72f)
            lineTo(size.width * 0.48f, size.height * 0.55f)
            lineTo(size.width * 0.12f, size.height * 0.82f)
            close()
        }
        drawPath(path = path, color = StatusGlyph)
        drawPath(path = path, color = StatusGlyph, style = stroke)
    }
}
