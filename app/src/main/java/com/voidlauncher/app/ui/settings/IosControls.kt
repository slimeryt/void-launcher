package com.voidlauncher.app.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.theme.IosBlue
import kotlin.math.roundToInt

private val ThumbShape = RoundedCornerShape(99.dp)

/** iOS-style switch with a wide capsule thumb (not a small circle). */
@Composable
fun IosToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackW = 58.dp
    val trackH = 32.dp
    val thumbW = 38.dp
    val thumbH = 28.dp
    val pad = 2.dp
    val offset by animateDpAsState(
        targetValue = if (checked) trackW - thumbW - pad else pad,
        animationSpec = tween(220),
        label = "ios-toggle"
    )
    val trackColor = when {
        !enabled -> Color(0xFF2C2C2E)
        checked -> IosBlue
        else -> Color(0xFF39393D)
    }
    Box(
        modifier = modifier
            .width(trackW)
            .height(trackH)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor)
            .pointerInput(checked, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { onCheckedChange(!checked) }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(thumbW)
                .height(thumbH)
                .shadow(3.dp, ThumbShape, clip = false)
                .clip(ThumbShape)
                .background(Color.White)
        )
    }
}

/** iOS-style slider with a wide capsule thumb. */
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val thumbW = 44.dp
    val thumbH = 24.dp
    val trackH = 6.dp
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)

    fun snap(raw: Float): Float {
        var f = raw.coerceIn(0f, 1f)
        if (steps > 0) {
            val n = steps + 1
            f = (f * n).roundToInt().coerceIn(0, n).toFloat() / n
        }
        return valueRange.start + f * rangeSpan
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbH + 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val thumbPx = with(density) { thumbW.toPx() }
        val travel = (widthPx - thumbPx).coerceAtLeast(1f)
        val frac = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

        fun setFromX(x: Float) {
            if (!enabled) return
            onValueChange(snap((x - thumbPx / 2f) / travel))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackH)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFF3A3A3C))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (enabled) IosBlue else IosBlue.copy(alpha = 0.4f))
            )
        }

        Box(
            modifier = Modifier
                .offset(x = with(density) { (frac * travel).toDp() })
                .align(Alignment.CenterStart)
                .width(thumbW)
                .height(thumbH)
                .shadow(4.dp, ThumbShape, clip = false)
                .clip(ThumbShape)
                .background(Color.White)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(valueRange, steps, enabled, widthPx) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> setFromX(offset.x) }
                }
                .pointerInput(valueRange, steps, enabled, widthPx) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        setFromX(change.position.x)
                    }
                }
        )
    }
}
