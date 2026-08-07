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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LiquidRefractionShader
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.LocalWallpaperScrollState
import com.voidlauncher.app.glass.WallpaperCrop
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
    // Stable holder — read .offset only inside graphicsLayer (no per-frame recomposition).
    val wallpaperScroll = LocalWallpaperScrollState.current
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val blurStrength = glass.blurStrength.coerceIn(0f, 1.6f)
    val frostAmount = glass.frostAmount
    val refractionOn = enableRefraction && glass.refractionEnabled
    val specularOn = enableSheen && glass.sheenEnabled

    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
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

    val blurSigma = when {
        blurStrength <= 0.01f -> 0f
        !useWallpaperBackdrop && refractionOn ->
            (6f * blurStrength).coerceIn(0f, 9f) * (if (strong) 1.05f else 1f)
        refractionOn -> (10f * blurStrength).coerceIn(0f, 16f) * (if (strong) 1.05f else 1f)
        else -> (22f * blurStrength).coerceIn(0f, 30f) * (if (strong) 1.05f else 0.92f)
    }
    val effectiveBlurSigma =
        if (useWallpaperBackdrop) blurSigma else (blurSigma * 0.45f).coerceAtMost(5f)

    val panelRenderEffect: RenderEffect? = remember(
        layerSize,
        effectiveBlurSigma,
        useOpticalShader,
        useWallpaperBackdrop,
        strong,
        blurStrength,
        frostAmount,
        specularOn,
        cornerRadiusPx,
        runtimeShader
    ) {
        if (layerSize.width <= 1 || layerSize.height <= 1) return@remember null
        val blurEffect =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && effectiveBlurSigma >= 1f) {
                AndroidRenderEffect.createBlurEffect(
                    effectiveBlurSigma,
                    effectiveBlurSigma,
                    Shader.TileMode.CLAMP
                )
            } else {
                null
            }
        val shader = runtimeShader
        val shaderEffect =
            if (useOpticalShader && shader != null) {
                val eta = if (useWallpaperBackdrop) {
                    (if (strong) 0.14f else 0.12f) *
                        (0.85f + 0.15f * blurStrength.coerceIn(0.4f, 1.4f))
                } else {
                    if (strong) 0.12f else 0.1f
                }
                LiquidRefractionShader.update(
                    shader = shader,
                    size = Size(layerSize.width.toFloat(), layerSize.height.toFloat()),
                    cornerRadiusPx = cornerRadiusPx,
                    eta = eta.coerceIn(0.07f, 0.18f),
                    frost = frostAmount * (if (useWallpaperBackdrop) {
                        if (strong) 0.85f else 0.7f
                    } else {
                        // Settings chrome: keep frost low so the plate stays transparent.
                        if (strong) 0.22f else 0.16f
                    }),
                    fresnelMin = if (useWallpaperBackdrop) 0.025f else 0.02f,
                    fresnelMax = if (useWallpaperBackdrop) {
                        if (strong) 0.28f else 0.22f
                    } else {
                        if (strong) 0.22f else 0.16f
                    },
                    specularPower = if (useWallpaperBackdrop) 56f else 48f,
                    specularStrength = if (specularOn) {
                        if (useWallpaperBackdrop) {
                            if (strong) 0.62f else 0.5f
                        } else {
                            if (strong) 0.55f else 0.42f
                        }
                    } else {
                        0f
                    },
                    chromatic = if (useWallpaperBackdrop) {
                        if (strong) 1.55f else 1.2f
                    } else {
                        if (strong) 1.5f else 1.2f
                    }
                )
                AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
            } else {
                null
            }
        when {
            shaderEffect != null && blurEffect != null ->
                AndroidRenderEffect.createChainEffect(shaderEffect, blurEffect)
                    .asComposeRenderEffect()
            shaderEffect != null -> shaderEffect.asComposeRenderEffect()
            blurEffect != null -> blurEffect.asComposeRenderEffect()
            else -> null
        }
    }

    val panelX = coords?.takeIf { it.isAttached }?.positionInWindow()?.x ?: 0f
    val panelY = coords?.takeIf { it.isAttached }?.positionInWindow()?.y ?: 0f
    val extraWidthPx = wallpaper?.let {
        (it.fullWidthPx - it.screenWidth).coerceAtLeast(0).toFloat()
    } ?: 0f
    val stripWidthPx = wallpaper?.fullWidthPx?.toFloat()?.coerceAtLeast(1f)
        ?: layerSize.width.toFloat().coerceAtLeast(1f)
    val stripHeightPx = layerSize.height.toFloat().coerceAtLeast(1f)
    val stripDp = with(density) {
        DpSize(stripWidthPx.toDp(), stripHeightPx.toDp())
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = when {
                    !useWallpaperBackdrop -> if (strong) 10.dp else 6.dp
                    strong -> 16.dp
                    else -> 8.dp
                },
                shape = shape,
                ambientColor = Color.Black.copy(
                    alpha = if (useWallpaperBackdrop) 0.28f else 0.16f
                ),
                spotColor = Color.Black.copy(
                    alpha = if (useWallpaperBackdrop) 0.38f else 0.2f
                ),
                clip = false
            )
            .clip(shape)
            .onGloballyPositioned { coords = it }
            .then(
                if (useOpticalShader) {
                    Modifier.liquidGlassStroke(cornerRadius = cornerRadius, strong = strong)
                } else {
                    Modifier.border(
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
                }
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { layerSize = it }
                .graphicsLayer(
                    renderEffect = panelRenderEffect,
                    compositingStrategy = CompositingStrategy.Offscreen
                )
        ) {
            if (useWallpaperBackdrop && wallpaper != null && layerSize.height > 1) {
                // Unbounded so the strip can be wider than the panel (matchParentSize
                // would otherwise crush small edit circles to 56×56 and kill refraction).
                Canvas(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                        .requiredSize(stripDp)
                        .graphicsLayer {
                            val originX = wallpaperScroll.offset.coerceIn(0f, 1f) * extraWidthPx
                            translationX = -(originX + panelX)
                        }
                ) {
                    val wp = wallpaper
                    val (_, originY) = WallpaperCrop.viewportOrigin(wp, wallpaperXOffset = 0.5f)
                    val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat().coerceAtLeast(1f)
                    val srcY = ((originY + panelY) * scaleY).roundToInt()
                        .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
                    val srcH = (stripHeightPx * scaleY).roundToInt().coerceAtLeast(1)
                        .coerceAtMost((wp.bitmapHeight - srcY).coerceAtLeast(1))
                    drawImage(
                        image = wp.image,
                        srcOffset = IntOffset(0, srcY),
                        srcSize = IntSize(wp.bitmapWidth.coerceAtLeast(1), srcH),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(
                            stripWidthPx.roundToInt().coerceAtLeast(1),
                            stripHeightPx.roundToInt().coerceAtLeast(1)
                        ),
                        alpha = 1f
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.minDimension <= 2f) return@Canvas
                    // Transparent liquid plate for settings chrome (see VoidInk / haze through).
                    drawRect(Color.White.copy(alpha = 0.10f))
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color(0xFF6EB0E8).copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.08f)
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width * 1.1f, size.height * 1.2f)
                        )
                    )
                    val bands = 10
                    for (i in 0 until bands) {
                        val y = size.height * (i + 0.5f) / bands
                        drawRect(
                            color = Color.White.copy(alpha = if (i % 2 == 0) 0.028f else 0.012f),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, 1.1f)
                        )
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.2f),
                            radius = size.minDimension * 0.95f
                        )
                    )
                    drawRect(Color.White.copy(alpha = 0.03f * frostAmount.coerceIn(0.3f, 1.5f)))
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
