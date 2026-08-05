package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Live liquid glass: blurred wallpaper sample + refraction.
 * Stack-blur buffer + GPU blur (API 31+) so frost always reads.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val blurred = LocalBlurredWallpaper.current
    val glass = LocalGlassSettings.current
    val effectiveRefraction = enableRefraction && glass.refractionEnabled
    val effectiveSheen = enableSheen && glass.sheenEnabled
    // Hoist for snapshot invalidation of graphicsLayer / drawBehind
    val blurStrength = glass.blurStrength
    val frostAmount = glass.frostAmount
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = remember(cornerRadius) { SmoothCornerShape(radius = cornerRadius) }

    // Always remember — never conditional (breaks when toggling refraction/sheen)
    val transition = rememberInfiniteTransition(label = "liquid")
    val sheenShift by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen"
    )
    val refractTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refract-time"
    )

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidRefractionShader.create() }.getOrNull()
        } else {
            null
        }
    }
    val useShader = effectiveRefraction && runtimeShader != null

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
        // Wallpaper sample as a child of graphicsLayer so AGSL RuntimeShader reliably samples it
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    // Read animated + settings values so this layer invalidates live
                    val tick = refractTime
                    val b = blurStrength
                    val f = frostAmount
                    val shaderOn = useShader

                    // Keep GPU blur light when refracting so the warp stays readable
                    val blurPx = if (shaderOn) {
                        (if (strong) 8f else 5f) * b
                    } else {
                        (if (strong) 20f else 12f) * b
                    }
                    val blurEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx >= 1f) {
                        AndroidRenderEffect.createBlurEffect(
                            blurPx,
                            blurPx,
                            Shader.TileMode.CLAMP
                        )
                    } else {
                        null
                    }
                    val shaderEffect =
                        if (shaderOn && runtimeShader != null &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            size.width > 1f && size.height > 1f
                        ) {
                            LiquidRefractionShader.update(
                                shader = runtimeShader,
                                size = Size(size.width, size.height),
                                intensity = (if (strong) 0.62f else 0.48f) * b.coerceIn(0.5f, 1.6f),
                                chromatic = if (strong) 0.028f else 0.018f,
                                frost = (if (strong) 0.22f else 0.12f) * f,
                                time = tick,
                                bezel = if (strong) 0.28f else 0.20f
                            )
                            AndroidRenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                        } else {
                            null
                        }
                    // Warp after a soft blur of the wallpaper sample
                    renderEffect = when {
                        shaderEffect != null && blurEffect != null ->
                            AndroidRenderEffect.createChainEffect(shaderEffect, blurEffect)
                                .asComposeRenderEffect()
                        shaderEffect != null -> shaderEffect.asComposeRenderEffect()
                        blurEffect != null -> blurEffect.asComposeRenderEffect()
                        else -> null
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val panel = coords
                val wp = blurred
                // Touch animated time so Canvas redraws with the CPU fallback / layout
                val tick = refractTime
                if (wp == null || panel == null || !panel.isAttached || size.minDimension <= 2f) {
                    return@Canvas
                }
                val pos = panel.positionInWindow()
                val scaleX = wp.bitmapWidth.toFloat() / wp.screenWidth.toFloat()
                val scaleY = wp.bitmapHeight.toFloat() / wp.screenHeight.toFloat()

                val pad = if (effectiveRefraction) 0.22f else 0.02f
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

                if (effectiveRefraction && !useShader) {
                    val breathe = 1.08f + 0.04f * sin(tick.toDouble()).toFloat()
                    val ca = 3.5f + 2.0f * cos(tick.toDouble()).toFloat()
                    withTransform({ scale(breathe, breathe, pivot = center) }) {
                        drawImage(
                            image = wp.image,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset((-ca).roundToInt(), (-ca * 0.3f).roundToInt()),
                            dstSize = dst,
                            alpha = 0.4f
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
                            alpha = 0.4f
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
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val hasWp = blurred != null && coords != null
                    if (hasWp) {
                        val frost = frostAmount
                        // Light veil — keep refraction readable (was washing the warp out)
                        drawRect(Color.White.copy(alpha = (if (strong) 0.10f else 0.06f) * frost))
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = (if (strong) 0.14f else 0.09f) * frost),
                                    Color.White.copy(alpha = (if (strong) 0.06f else 0.04f) * frost),
                                    Color.Black.copy(alpha = (if (strong) 0.05f else 0.03f) * frost)
                                )
                            )
                        )
                        if (tint.alpha > 0.01f) drawRect(tint)
                        if (effectiveSheen) {
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
                                    Color.White.copy(alpha = 0.75f),
                                    VoidCyan.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.22f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    } else {
                        drawRect(Color.White.copy(alpha = if (strong) 0.22f else 0.14f))
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (strong) 0.26f else 0.16f),
                                    Color.White.copy(alpha = if (strong) 0.14f else 0.08f),
                                    Color.Black.copy(alpha = 0.08f)
                                )
                            )
                        )
                        if (tint.alpha > 0.01f) drawRect(tint)
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
