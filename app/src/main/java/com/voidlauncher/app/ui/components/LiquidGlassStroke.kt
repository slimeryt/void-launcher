package com.voidlauncher.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-like Liquid Glass edge: luminous outer rim, dark inner hairline, bright top lip.
 */
fun Modifier.liquidGlassStroke(
    cornerRadius: Dp,
    strong: Boolean = true
): Modifier = drawWithContent {
    drawContent()
    val crPx = cornerRadius.toPx()
    val cr = CornerRadius(crPx, crPx)
    val inset = 0.75.dp.toPx()
    drawRoundRect(
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
        cornerRadius = cr,
        style = Stroke(width = 1.35.dp.toPx())
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.28f),
        topLeft = Offset(inset, inset),
        size = Size(
            (size.width - inset * 2f).coerceAtLeast(0f),
            (size.height - inset * 2f).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (crPx - inset).coerceAtLeast(1f),
            (crPx - inset).coerceAtLeast(1f)
        ),
        style = Stroke(width = 0.7.dp.toPx())
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.45f
        ),
        cornerRadius = cr,
        style = Stroke(width = 0.9.dp.toPx())
    )
}

/** Same stroke when corner radius is a fraction of the icon’s short side (0–0.5). */
fun Modifier.liquidGlassStrokeRatio(
    cornerRadiusRatio: Float,
    strong: Boolean = true
): Modifier = drawWithContent {
    drawContent()
    val crPx = size.minDimension * cornerRadiusRatio.coerceIn(0f, 0.5f)
    val cr = CornerRadius(crPx, crPx)
    val inset = 0.75.dp.toPx()
    drawRoundRect(
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
        cornerRadius = cr,
        style = Stroke(width = 1.35.dp.toPx())
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.28f),
        topLeft = Offset(inset, inset),
        size = Size(
            (size.width - inset * 2f).coerceAtLeast(0f),
            (size.height - inset * 2f).coerceAtLeast(0f)
        ),
        cornerRadius = CornerRadius(
            (crPx - inset).coerceAtLeast(1f),
            (crPx - inset).coerceAtLeast(1f)
        ),
        style = Stroke(width = 0.7.dp.toPx())
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.45f
        ),
        cornerRadius = cr,
        style = Stroke(width = 0.9.dp.toPx())
    )
}
