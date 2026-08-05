package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.ui.theme.VoidCyan
import com.voidlauncher.app.ui.theme.VoidGlass
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import com.voidlauncher.app.ui.theme.VoidGlassStrong
import kotlin.math.roundToInt

/**
 * Live liquid glass: samples blurred wallpaper at this panel's screen position,
 * then adds frost / specular / rim light. Refraction is a subtle edge highlight
 * only — no RGB ghosting or heavy lens warp (those looked like a stamped image).
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

    val sheenShift by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen-shift"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { coords = it }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        VoidGlassBorder,
                        Color(0x33FFFFFF),
                        Color(0x11FFFFFF)
                    )
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val panel = coords
                    val wp = blurred
                    if (wp != null && panel != null && panel.isAttached && size.minDimension > 2f) {
                        val pos = panel.positionInWindow()
                        val scaleX = wp.bitmapWidth.toFloat() / wp.screenWidth.toFloat()
                        val scaleY = wp.bitmapHeight.toFloat() / wp.screenHeight.toFloat()

                        val srcX = (pos.x * scaleX).roundToInt()
                            .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
                        val srcY = (pos.y * scaleY).roundToInt()
                            .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
                        val srcW = (size.width * scaleX).roundToInt().coerceAtLeast(1)
                            .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
                        val srcH = (size.height * scaleY).roundToInt().coerceAtLeast(1)
                            .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))

                        // Soft wallpaper sample (slightly expanded for frosted feel)
                        drawImage(
                            image = wp.image,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(
                                size.width.roundToInt().coerceAtLeast(1),
                                size.height.roundToInt().coerceAtLeast(1)
                            ),
                            alpha = 0.85f
                        )

                        // Heavy frost so it reads as glass, not a photo crop
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (strong) 0.38f else 0.26f),
                                    Color.White.copy(alpha = if (strong) 0.16f else 0.10f),
                                    Color(0xFF0A0C12).copy(alpha = if (strong) 0.22f else 0.14f)
                                )
                            )
                        )

                        // Specular
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.White.copy(alpha = 0.35f),
                                    0.22f to Color.White.copy(alpha = 0.08f),
                                    0.5f to Color.Transparent
                                )
                            ),
                            size = Size(size.width, size.height * 0.5f)
                        )

                        if (enableSheen) {
                            val bandX = size.width * sheenShift
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    start = Offset(bandX - size.width * 0.2f, 0f),
                                    end = Offset(bandX + size.width * 0.2f, size.height)
                                )
                            )
                        }

                        // Subtle refractive rim (no RGB split / lens warp)
                        if (enableRefraction) {
                            val stroke = 1.15.dp.toPx()
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        VoidCyan.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.18f)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                ),
                                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                                style = Stroke(width = stroke)
                            )
                        }
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
