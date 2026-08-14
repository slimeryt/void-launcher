package com.voidlauncher.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-like Liquid Glass edge: luminous rim + bright top lip, following [shape]
 * (must match the clip / mask — e.g. [SmoothCornerShape] for icons).
 *
 * Place this **outside** `.clip(shape)` so the rim isn’t cropped mid-stroke.
 */
fun Modifier.liquidGlassStroke(
    shape: Shape,
    strong: Boolean = true
): Modifier = drawWithContent {
    drawContent()
    val path = shape.createOutline(size, layoutDirection, this).toPath()
    val rim = 1.2.dp.toPx()
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (strong) 0.72f else 0.58f),
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.22f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        style = Stroke(width = rim, join = StrokeJoin.Round)
    )
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.45f
        ),
        style = Stroke(width = 0.85.dp.toPx(), join = StrokeJoin.Round)
    )
}

/** Convenience for panels that only know a corner radius in Dp. */
fun Modifier.liquidGlassStroke(
    cornerRadius: Dp,
    strong: Boolean = true
): Modifier = liquidGlassStroke(
    shape = SmoothCornerShape(radius = cornerRadius),
    strong = strong
)

/** Prefer [liquidGlassStroke] with the real [Shape] when available. */
fun Modifier.liquidGlassStrokeRatio(
    cornerRadiusRatio: Float,
    strong: Boolean = true
): Modifier = liquidGlassStroke(
    shape = SmoothCornerShape(
        percent = (cornerRadiusRatio.coerceIn(0f, 0.5f) * 100f).toInt().coerceIn(0, 50)
    ),
    strong = strong
)

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}
