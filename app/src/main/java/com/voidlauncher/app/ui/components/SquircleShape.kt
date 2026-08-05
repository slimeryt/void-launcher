package com.voidlauncher.app.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

/** Default iOS / Figma-like corner smoothing (~60%). */
const val DefaultCornerSmoothing = 0.6f

/**
 * Continuous (smoothed) rounded rect — Figma-style corner smoothing via
 * [androidx.graphics.shapes], not a plain circular RoundedCornerShape.
 *
 * @param smoothing 0 = circular corners, 1 = max continuous flanking curves. Prefer ~0.6.
 */
class SmoothCornerShape(
    private val topStart: CornerSize,
    private val topEnd: CornerSize,
    private val bottomEnd: CornerSize,
    private val bottomStart: CornerSize,
    private val smoothing: Float = DefaultCornerSmoothing
) : Shape {

    constructor(
        corner: CornerSize,
        smoothing: Float = DefaultCornerSmoothing
    ) : this(corner, corner, corner, corner, smoothing)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect.Zero)
        }

        val maxR = size.minDimension / 2f
        val sm = smoothing.coerceIn(0f, 1f)

        var ts = topStart.toPx(size, density).coerceIn(0f, maxR)
        var te = topEnd.toPx(size, density).coerceIn(0f, maxR)
        var be = bottomEnd.toPx(size, density).coerceIn(0f, maxR)
        var bs = bottomStart.toPx(size, density).coerceIn(0f, maxR)

        if (layoutDirection == LayoutDirection.Rtl) {
            val tmpTop = ts
            ts = te
            te = tmpTop
            val tmpBottom = bs
            bs = be
            be = tmpBottom
        }

        if (ts == te && te == be && be == bs) {
            if (ts <= 0.5f) {
                return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
            }
            val polygon = RoundedPolygon.rectangle(
                width = size.width,
                height = size.height,
                rounding = CornerRounding(ts, sm),
                centerX = size.width / 2f,
                centerY = size.height / 2f
            )
            return Outline.Generic(polygon.toComposePath())
        }

        val polygon = RoundedPolygon(
            vertices = floatArrayOf(
                0f, 0f,
                size.width, 0f,
                size.width, size.height,
                0f, size.height
            ),
            perVertexRounding = listOf(
                CornerRounding(ts, sm),
                CornerRounding(te, sm),
                CornerRounding(be, sm),
                CornerRounding(bs, sm)
            )
        )
        return Outline.Generic(polygon.toComposePath())
    }
}

fun SmoothCornerShape(
    radius: Dp,
    smoothing: Float = DefaultCornerSmoothing
): SmoothCornerShape = SmoothCornerShape(CornerSize(radius), smoothing)

fun SmoothCornerShape(
    percent: Int,
    smoothing: Float = DefaultCornerSmoothing
): SmoothCornerShape = SmoothCornerShape(CornerSize(percent), smoothing)

/** Stadium / pill with continuous end caps. */
val CapsuleShape: Shape = SmoothCornerShape(percent = 50)

fun CapsuleShape(smoothing: Float): Shape =
    SmoothCornerShape(percent = 50, smoothing = smoothing)

/** App / folder icon mask — continuous corners at ~24% of min side. */
val AppIconShape: Shape = SmoothCornerShape(percent = 24)

/** @deprecated Prefer [AppIconShape]. */
val IosSquircle: Shape = AppIconShape

val IosContinuousCorner: Shape = AppIconShape

private fun RoundedPolygon.toComposePath(): Path {
    val ag = android.graphics.Path()
    toPath(ag)
    return ag.asComposePath()
}
