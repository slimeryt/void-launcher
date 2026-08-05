package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LiquidRefractionShader
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidGlass
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import com.voidlauncher.app.ui.theme.VoidGlassStrong
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Live liquid glass: clear wallpaper sample + visible refraction.
 * AGSL (API 33+) with Offscreen compositing; CPU lens+CA fallback always readable.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    /** Optional wash on top of glass (e.g. iOS blue for Done). */
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val blurred = LocalBlurredWallpaper.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(cornerRadius)

    val needsMotion = enableSheen || enableRefraction
    val transition = if (needsMotion) {
        rememberInfiniteTransition(label = "liquid")
    } else {
        null
    }
    val sheenShift by if (transition != null) {
        transition.animateFloat(
            initialValue = -0.15f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(6400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sheen"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }
    val refractTime by if (transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "refract-time"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val runtimeShader = remember(enableRefraction) {
        if (enableRefraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidRefractionShader.create() }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { coords = it }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        VoidGlassBorder,
                        Color(0x66FFFFFF),
                        Color(0x22FFFFFF)
                    )
                ),
                shape = shape
            )
    ) {
        // Wallpaper + refraction (must be Offscreen for RuntimeShader RenderEffect)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Modifier.graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            LiquidRefractionShader.update(
                                shader = runtimeShader,
                                size = Size(size.width, size.height),
                                intensity = if (strong) 0.28f else 0.22f,
                                chromatic = if (strong) 0.020f else 0.014f,
                                frost = 0f,
                                time = refractTime
                            )
                            renderEffect = AndroidRenderEffect
                                .createRuntimeShaderEffect(runtimeShader, "content")
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .drawBehind {
                    val panel = coords
                    val wp = blurred
                    if (wp == null || panel == null || !panel.isAttached || size.minDimension <= 2f) {
                        return@drawBehind
                    }
                    val pos = panel.positionInWindow()
                    val scaleX = wp.bitmapWidth.toFloat() / wp.screenWidth.toFloat()
                    val scaleY = wp.bitmapHeight.toFloat() / wp.screenHeight.toFloat()

                    val pad = if (enableRefraction) 0.12f else 0.02f
                    val srcX = ((pos.x - size.width * pad) * scaleX).roundToInt()
                        .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
                    val srcY = ((pos.y - size.height * pad) * scaleY).roundToInt()
                        .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
                    val srcW = ((size.width * (1f + pad * 2f)) * scaleX).roundToInt().coerceAtLeast(1)
                        .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
                    val srcH = ((size.height * (1f + pad * 2f)) * scaleY).roundToInt().coerceAtLeast(1)
                        .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))

                    val dst = IntSize(
                        size.width.roundToInt().coerceAtLeast(1),
                        size.height.roundToInt().coerceAtLeast(1)
                    )

                    if (enableRefraction) {
                        val breathe = 1.08f + 0.035f * sin(refractTime.toDouble()).toFloat()
                        val ca = 3.5f + 1.5f * cos(refractTime.toDouble()).toFloat()
                        withTransform({ scale(breathe, breathe, pivot = center) }) {
                            drawImage(
                                image = wp.image,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(srcW, srcH),
                                dstOffset = IntOffset((-ca).roundToInt(), (-ca * 0.3f).roundToInt()),
                                dstSize = dst,
                                alpha = 0.40f
                            )
                            drawImage(
                                image = wp.image,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(srcW, srcH),
                                dstOffset = IntOffset.Zero,
                                dstSize = dst,
                                alpha = 1f
                            )
                            drawImage(
                                image = wp.image,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(srcW, srcH),
                                dstOffset = IntOffset(ca.roundToInt(), (ca * 0.3f).roundToInt()),
                                dstSize = dst,
                                alpha = 0.40f
                            )
                        }
                    } else {
                        drawImage(
                            image = wp.image,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset.Zero,
                            dstSize = dst,
                            alpha = 1f
                        )
                    }
                }
        )

        // Clear frost / sheen / tint — keep glass readable, not milky
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val hasWp = blurred != null && coords != null
                    if (hasWp) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (strong) 0.12f else 0.07f),
                                    Color.White.copy(alpha = if (strong) 0.05f else 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                        if (tint.alpha > 0.01f) {
                            drawRect(tint)
                        }
                        if (enableSheen) {
                            val bandX = size.width * sheenShift
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    start = Offset(bandX - size.width * 0.2f, 0f),
                                    end = Offset(bandX + size.width * 0.2f, size.height)
                                )
                            )
                        }
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    VoidCyan.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.22f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    } else {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (strong) 0.16f else 0.10f),
                                    Color.White.copy(alpha = if (strong) 0.08f else 0.05f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            )
                        )
                        if (tint.alpha > 0.01f) drawRect(tint)
                        if (enableSheen) {
                            val bandX = size.width * sheenShift
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    start = Offset(bandX - size.width * 0.2f, 0f),
                                    end = Offset(bandX + size.width * 0.2f, size.height)
                                )
                            )
                        }
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    VoidCyan.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.25f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = Stroke(width = 1.1.dp.toPx())
                        )
                    }
                }
        )

        content()
    }
}

/** Force every icon into a rounded rectangle bitmap (no leftover circular masks). */
fun Drawable.toCachedBitmap(maxSize: Int = 192, cornerRadiusRatio: Float = 0.22f): Bitmap {
    val size = maxSize.coerceAtLeast(48)
    val raw = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rawCanvas = AndroidCanvas(raw)

    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable -> {
            val inset = (size * 0.22f).roundToInt()
            background?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(rawCanvas)
            }
            foreground?.let {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(rawCanvas)
            }
        }
        this is BitmapDrawable && bitmap != null && !bitmap.isRecycled -> {
            val src = bitmap
            val zoom = (size * 1.2f).roundToInt()
            val o = (size - zoom) / 2
            val scaled = Bitmap.createScaledBitmap(src, zoom, zoom, true)
            rawCanvas.drawBitmap(scaled, o.toFloat(), o.toFloat(), null)
            if (scaled !== src) scaled.recycle()
        }
        else -> {
            val zoom = (size * 1.2f).roundToInt()
            val o = (size - zoom) / 2
            setBounds(o, o, o + zoom, o + zoom)
            draw(rawCanvas)
        }
    }

    // Mask to rounded rect so the shape is baked in for every app
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val outCanvas = AndroidCanvas(out)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    val path = AndroidPath().apply {
        val r = size * cornerRadiusRatio
        addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, AndroidPath.Direction.CW)
    }
    outCanvas.drawPath(path, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    outCanvas.drawBitmap(raw, 0f, 0f, paint)
    raw.recycle()
    return out
}
