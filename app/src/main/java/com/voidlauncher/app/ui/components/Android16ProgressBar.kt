package com.voidlauncher.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android 16-style segmented progress (split track + moving head when indeterminate).
 * [progress] in 0..1 for determinate; use a negative value for indeterminate sweep.
 */
@Composable
fun Android16ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    activeColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.28f)
) {
    val indeterminate = rememberInfiniteTransition(label = "a16-progress")
    val sweep by indeterminate.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val gap = 4.dp.toPx()
        val r = size.height / 2f

        if (progress < 0f) {
            val headW = (size.width * 0.32f).coerceAtLeast(size.height * 3f)
            val travel = size.width + headW + gap
            val headEnd = (sweep * travel) - gap
            val headStart = headEnd - headW

            val leftEnd = (headStart - gap).coerceAtMost(size.width)
            if (leftEnd > 0f) {
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset.Zero,
                    size = Size(leftEnd.coerceAtLeast(0f), size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            val drawStart = headStart.coerceIn(0f, size.width)
            val drawEnd = headEnd.coerceIn(0f, size.width)
            if (drawEnd > drawStart) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(drawStart, 0f),
                    size = Size(drawEnd - drawStart, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            val rightStart = (headEnd + gap).coerceAtLeast(0f)
            if (rightStart < size.width) {
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(rightStart, 0f),
                    size = Size(size.width - rightStart, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
        } else {
            val p = progress.coerceIn(0f, 1f)
            if (p >= 0.999f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            } else {
                val activeW = ((size.width - gap) * p).coerceAtLeast(0f)
                if (activeW > 0.5f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset.Zero,
                        size = Size(activeW, size.height),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                val restStart = activeW + gap
                if (restStart < size.width) {
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(restStart, 0f),
                        size = Size(size.width - restStart, size.height),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
        }
    }
}
