package com.voidlauncher.app.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.ui.liquid.utils.DampedDragAnimation
import com.voidlauncher.app.ui.theme.IosBlue
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * Kyant-style liquid slider: white capsule at rest, liquid glass while held.
 * Thumb position is local (snapped) so drag stays smooth.
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
    val accentColor = if (enabled) IosBlue else IosBlue.copy(alpha = 0.4f)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(alpha = 0.2f)
        else Color(0xFF787880).copy(alpha = 0.36f)
    val glass = LocalGlassSettings.current

    val trackBackdrop = rememberLayerBackdrop()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }

    fun snap(raw: Float): Float {
        var f = ((raw - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            .coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
        if (steps > 0) {
            val n = steps + 1
            f = (f * n).roundToInt().coerceIn(0, n).toFloat() / n
        }
        return valueRange.start + f * (valueRange.endInclusive - valueRange.start)
    }

    val dampedDragAnimation = remember(animationScope, valueRange) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = value(),
            valueRange = valueRange,
            visibilityThreshold = visibilityThreshold,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    onValueChange(snap(targetValue))
                    didDrag = false
                }
            },
            onDrag = { size, dragAmount ->
                if (enabled) {
                    if (!didDrag) didDrag = dragAmount.x != 0f
                    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
                    val delta = span * (dragAmount.x / size.width.coerceAtLeast(1))
                    val next = if (isLtr) targetValue + delta else targetValue - delta
                    val coerced = next.coerceIn(valueRange.start, valueRange.endInclusive)
                    snapValue(coerced)
                    onValueChange(coerced)
                }
            }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { value() }
            .collectLatest { incoming ->
                if (dampedDragAnimation.pressProgress < 0.01f &&
                    dampedDragAnimation.targetValue != incoming
                ) {
                    dampedDragAnimation.updateValue(incoming)
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(trackColor)
                    .pointerInput(enabled, valueRange, steps, trackWidth) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { position ->
                            val span = (valueRange.endInclusive - valueRange.start)
                                .coerceAtLeast(0.0001f)
                            val raw = if (isLtr) {
                                valueRange.start + span * (position.x / trackWidth)
                            } else {
                                valueRange.endInclusive - span * (position.x / trackWidth)
                            }
                            val target = snap(raw.coerceIn(valueRange.start, valueRange.endInclusive))
                            dampedDragAnimation.animateToValue(target)
                            onValueChange(target)
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth()
            )
            Box(
                Modifier
                    .fillMaxWidth(dampedDragAnimation.progress.coerceAtLeast(0.001f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accentColor)
            )
        }

        val thumbBackdrop = rememberCombinedBackdrop(
            backdrop,
            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                val progress = dampedDragAnimation.pressProgress
                val scaleX = lerp(2f / 3f, 1f, progress)
                val scaleY = lerp(0f, 1f, progress)
                scale(scaleX, scaleY) { drawBackdrop() }
            }
        )

        Box(
            Modifier
                .graphicsLayer {
                    val w = size.width
                    translationX =
                        (-w / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-w / 4f, trackWidth - w * 3f / 4f) *
                            if (isLtr) 1f else -1f
                }
                .then(if (enabled) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = thumbBackdrop,
                    shape = { RoundedCornerShape(percent = 50) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        if (glass.refractionEnabled) {
                            lens(
                                refractionHeight = glass.refractionHeightDp.dp.toPx() * progress,
                                refractionAmount = glass.refractionAmountDp.dp.toPx() * progress,
                                chromaticAberration = glass.chromaticAberration
                            )
                        }
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = layer@{
                        val w = size.width
                        val h = size.height
                        if (w < 1f || h < 1f) return@layer
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40.dp, 24.dp)
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
