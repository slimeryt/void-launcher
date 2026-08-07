package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LiquidRefractionShader
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import kotlin.math.roundToInt

/**
 * Optical liquid glass (Polar AGSL): wallpaper/plate backdrop → light blur →
 * RuntimeShader refraction / chromatic / Fresnel / specular.
 *
 * [sampleWallpaper] true = wallpaper crop (home / preview). false = frost plate
 * for settings chrome (no wallpaper portals).
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    sampleWallpaper: Boolean = true,
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaper = LocalBlurredWallpaper.current
    val glass = LocalGlassSettings.current
    val wallpaperXOffset = LocalWallpaperXOffset.current
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val blurStrength = glass.blurStrength.coerceIn(0f, 1.6f)
    val frostAmount = glass.frostAmount
    val refractionOn = enableRefraction && glass.refractionEnabled
    val specularOn = enableSheen && glass.sheenEnabled

    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = remember(cornerRadius) { SmoothCornerShape(radius = cornerRadius) }

    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidRefractionShader.create() }
                .onFailure { Log.e("GlassPanel", "AGSL liquid glass failed to compile", it) }
                .getOrNull()
        } else {
            null
        }
    }
    val useWallpaperBackdrop = sampleWallpaper && wallpaper != null
    val useOpticalShader = refractionOn && runtimeShader != null

    // iOS-like: light frost blur so magnification/rim lens stay sharp.
    val blurSigma = when {
        blurStrength <= 0.01f -> 0f
        refractionOn -> (10f * blurStrength).coerceIn(0f, 16f) * (if (strong) 1.05f else 1f)
        else -> (22f * blurStrength).coerceIn(0f, 30f) * (if (strong) 1.05f else 0.92f)
    }
    val effectiveBlurSigma =
        if (useWallpaperBackdrop) blurSigma else (blurSigma * 0.35f).coerceAtMost(7f)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (strong) 16.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.38f),
                clip = false
            )
            .clip(shape)
            .onGloballyPositioned { coords = it }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        VoidGlassBorder.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.14f)
                    )
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    val blurPx = effectiveBlurSigma
                    val blurEffect =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx >= 1f) {
                            AndroidRenderEffect.createBlurEffect(
                                blurPx,
                                blurPx,
                                Shader.TileMode.CLAMP
                            )
                        } else {
                            null
                        }

                    val shader = runtimeShader
                    val shaderEffect =
                        if (useOpticalShader && shader != null && size.width > 1f && size.height > 1f) {
                            val eta = if (useWallpaperBackdrop) {
                                (if (strong) 0.12f else 0.1f) *
                                    (0.75f + 0.25f * blurStrength.coerceIn(0.4f, 1.4f))
                            } else {
                                0.06f
                            }
                            LiquidRefractionShader.update(
                                shader = shader,
                                size = Size(size.width, size.height),
                                cornerRadiusPx = cornerRadiusPx,
                                eta = eta.coerceIn(0.05f, 0.15f),
                                frost = frostAmount * (if (strong) 0.9f else 0.75f),
                                fresnelMin = 0.04f,
                                fresnelMax = if (strong) 0.34f else 0.28f,
                                specularPower = 48f,
                                specularStrength = if (specularOn) {
                                    if (strong) 0.58f else 0.45f
                                } else {
                                    0f
                                },
                                // iOS fringe is subtle — was over-prismatic before.
                                chromatic = if (useWallpaperBackdrop) {
                                    if (strong) 1.6f else 1.2f
                                } else {
                                    0.7f
                                }
                            )
                            AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
                        } else {
                            null
                        }

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
                if (size.minDimension <= 2f) return@Canvas
                if (useWallpaperBackdrop) {
                    val panel = coords
                    val wp = wallpaper ?: return@Canvas
                    if (panel == null || !panel.isAttached) return@Canvas
                    val pos = panel.positionInWindow()
                    val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat()
                    val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat()
                    val extraWidthPx = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
                    val pageOffsetPx = wallpaperXOffset.coerceIn(0f, 1f) * extraWidthPx
                    val realX = pageOffsetPx + pos.x
                    val pad = 0.22f
                    val srcX = ((realX - size.width * pad) * scaleX).roundToInt()
                        .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
                    val srcY = ((pos.y - size.height * pad) * scaleY).roundToInt()
                        .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
                    val srcW = ((size.width * (1f + pad * 2f)) * scaleX).roundToInt().coerceAtLeast(1)
                        .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
                    val srcH = ((size.height * (1f + pad * 2f)) * scaleY).roundToInt().coerceAtLeast(1)
                        .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))
                    drawImage(
                        image = wp.image,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(srcW, srcH),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(
                            size.width.roundToInt().coerceAtLeast(1),
                            size.height.roundToInt().coerceAtLeast(1)
                        ),
                        alpha = 1f
                    )
                } else {
                    drawRect(Color(0xFF1C1C1E))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color(0xFF4A90D9).copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.08f)
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                    drawRect(Color.White.copy(alpha = 0.08f * frostAmount.coerceIn(0.3f, 1.5f)))
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    if (!useOpticalShader) {
                        if (!useWallpaperBackdrop) {
                            val veil = if (strong) 0.22f else 0.16f
                            drawRect(Color.White.copy(alpha = veil * frostAmount.coerceIn(0.3f, 1.5f)))
                        } else {
                            drawRect(Color.White.copy(alpha = 0.06f * frostAmount))
                        }
                        val rim = if (strong) 0.28f else 0.18f
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rim),
                                    Color.Transparent,
                                    Color.White.copy(alpha = rim * 0.55f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    if (tint.alpha > 0.01f) drawRect(tint)
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
    val path = continuousRoundedRectPath(
        width = size.toFloat(),
        height = size.toFloat(),
        cornerRadius = size * cornerRadiusRatio.coerceIn(0f, 0.5f)
    )
    outCanvas.drawPath(path, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    outCanvas.drawBitmap(raw, 0f, 0f, paint)
    raw.recycle()
    return out
}
