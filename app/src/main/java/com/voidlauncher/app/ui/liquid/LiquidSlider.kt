package com.voidlauncher.app.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.ui.theme.IosBlue
import kotlin.math.roundToInt

/**
 * Liquid-glass slider: Kyant thumb + a full-track gesture overlay so drag never stalls.
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val isLightTheme = !isSystemInDarkTheme()
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(alpha = 0.2f)
        else Color(0xFF787880).copy(alpha = 0.36f)
    val fillColor = if (enabled) IosBlue else IosBlue.copy(alpha = 0.4f)
    val glass = LocalGlassSettings.current

    val density = LocalDensity.current
    val thumbW = 40.dp
    val thumbH = 24.dp
    val trackH = 6.dp
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val current = value()
    val trackBackdrop = rememberLayerBackdrop()
    val thumbBackdrop = rememberCombinedBackdrop(backdrop, trackBackdrop)

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
        val frac = ((current - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

        fun setFromX(x: Float) {
            if (!enabled) return
            onValueChange(snap((x - thumbPx / 2f) / travel))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackH)
                .align(Alignment.Center)
                .layerBackdrop(trackBackdrop)
                .clip(RoundedCornerShape(percent = 50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(fillColor)
            )
        }

        key(
            glass.blurRadiusDp,
            glass.refractionHeightDp,
            glass.refractionAmountDp,
            glass.chromaticAberration,
            glass.refractionEnabled
        ) {
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (frac * travel).toDp() })
                    .align(Alignment.CenterStart)
                    .width(thumbW)
                    .height(thumbH)
                    .drawBackdrop(
                        backdrop = thumbBackdrop,
                        shape = { RoundedCornerShape(percent = 50) },
                        effects = {
                            vibrancy()
                            val blurDp = (glass.blurRadiusDp * 0.45f).coerceIn(2f, 18f)
                            blur(blurDp.dp.toPx())
                            if (glass.refractionEnabled) {
                                lens(
                                    refractionHeight = (glass.refractionHeightDp * 0.55f)
                                        .coerceIn(4f, 22f).dp.toPx(),
                                    refractionAmount = (glass.refractionAmountDp * 0.45f)
                                        .coerceIn(8f, 36f).dp.toPx(),
                                    chromaticAberration = glass.chromaticAberration
                                )
                            }
                        },
                        highlight = { Highlight.Default },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.22f))
                        }
                    )
            )
        }

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

@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true
) {
    LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        visibilityThreshold = 0.001f,
        backdrop = backdrop,
        modifier = modifier,
        steps = steps,
        enabled = enabled
    )
}
