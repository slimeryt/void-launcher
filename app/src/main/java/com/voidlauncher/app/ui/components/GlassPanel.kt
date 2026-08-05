package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.RenderEffect as AndroidRenderEffect
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
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Live liquid glass with AGSL refraction (API 33+) of the wallpaper behind the panel.
 * Pre-33 falls back to a mild zoom+offset sample so refraction still reads.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val blurred = LocalBlurredWallpaper.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = RoundedCornerShape(cornerRadius)

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
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refract-time"
    )

    val runtimeShader = remember(enableRefraction) {
        if (enableRefraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidRefractionShader.create()
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
                        Color(0x44FFFFFF),
                        Color(0x18FFFFFF)
                    )
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Modifier.graphicsLayer {
                            LiquidRefractionShader.update(
                                shader = runtimeShader,
                                size = Size(size.width, size.height),
                                // Stronger lens so wallpaper refraction reads clearly on dock/pill
                                intensity = if (strong) 0.32f else 0.24f,
                                chromatic = if (strong) 0.022f else 0.016f,
                                frost = if (strong) 0.28f else 0.20f,
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
                    if (wp != null && panel != null && panel.isAttached && size.minDimension > 2f) {
                        val pos = panel.positionInWindow()
                        val scaleX = wp.bitmapWidth.toFloat() / wp.screenWidth.toFloat()
                        val scaleY = wp.bitmapHeight.toFloat() / wp.screenHeight.toFloat()

                        // Sample a slightly larger region so lens warp has pixels to pull from
                        val pad = if (enableRefraction) 0.08f else 0f
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

                        val useCpuLens = enableRefraction && runtimeShader == null
                        if (useCpuLens) {
                            val zoom = 1.06f + 0.015f * sin(refractTime.toDouble()).toFloat()
                            withTransform({
                                scale(zoom, zoom, pivot = center)
                            }) {
                                drawImage(
                                    image = wp.image,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(srcW, srcH),
                                    dstOffset = IntOffset.Zero,
                                    dstSize = dst,
                                    alpha = 0.95f
                                )
                            }
                            // Light frost only — keep warped wallpaper visible
                            drawRect(Color.White.copy(alpha = if (strong) 0.18f else 0.12f))
                        } else {
                            drawImage(
                                image = wp.image,
                                srcOffset = IntOffset(srcX, srcY),
                                srcSize = IntSize(srcW, srcH),
                                dstOffset = IntOffset.Zero,
                                dstSize = dst,
                                alpha = 1f
                            )
                            // When AGSL handles frost, keep overlay light
                            if (runtimeShader == null) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (strong) 0.28f else 0.18f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                            }
                        }

                        if (enableSheen && runtimeShader == null) {
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
                                    Color.White.copy(alpha = 0.7f),
                                    VoidCyan.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.2f)
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
                                    if (strong) VoidGlassStrong else VoidGlass,
                                    Color(0x22FFFFFF)
                                )
                            )
                        )
                    }
                }
        )
        content()
    }
}

fun Drawable.toCachedBitmap(maxSize: Int = 192): Bitmap {
    if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
        val src = bitmap
        if (src.width <= maxSize && src.height <= maxSize) return src
        val scale = maxSize.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(
            src,
            (src.width * scale).roundToInt().coerceAtLeast(1),
            (src.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }
    val w = intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(maxSize) ?: 96
    val h = intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(maxSize) ?: 96
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}
