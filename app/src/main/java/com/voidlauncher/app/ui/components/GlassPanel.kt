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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LiquidRefractionShader
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import com.voidlauncher.app.ui.theme.VoidInk
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt

/**
 * Liquid glass that blurs/refracts whatever is actually behind the panel.
 *
 * Prefers [LocalHazeState] backdrop blur (real Compose content). Falls back to
 * wallpaper sampling only when no Haze host is provided (legacy / preview).
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    /**
     * When true, sample the wallpaper buffer instead of Haze (Liquid Glass live preview).
     * Everywhere else, omit this so glass blurs whatever is actually behind the panel.
     */
    sampleWallpaper: Boolean = false,
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val useHaze = hazeState != null && !sampleWallpaper
    val blurred = when {
        sampleWallpaper -> LocalBlurredWallpaper.current
        useHaze -> null
        else -> LocalBlurredWallpaper.current
    }
    val glass = LocalGlassSettings.current
    val wallpaperXOffset = LocalWallpaperXOffset.current
    val effectiveRefraction = enableRefraction && glass.refractionEnabled && blurred != null
    val effectiveSheen = enableSheen && glass.sheenEnabled
    val blurStrength = glass.blurStrength
    val frostAmount = glass.frostAmount
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val shape = remember(cornerRadius) { SmoothCornerShape(radius = cornerRadius) }

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
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidRefractionShader.create() }
                .onFailure { Log.e("GlassPanel", "AGSL refraction shader failed to compile", it) }
                .getOrNull()
        } else {
            null
        }
    }
    val useShader = effectiveRefraction && runtimeShader != null

    val hazeStyle = remember(blurStrength, frostAmount, strong, tint) {
        val blurRadius = ((if (strong) 28f else 18f) * blurStrength.coerceIn(0.35f, 1.6f)).dp
        val frost = frostAmount.coerceIn(0f, 1.5f)
        val baseTint = Color.White.copy(alpha = (if (strong) 0.14f else 0.10f) * frost)
        val tintLayer = if (tint.alpha > 0.01f) tint else Color.Transparent
        HazeStyle(
            backgroundColor = VoidInk.copy(alpha = 0.35f),
            tints = buildList {
                add(HazeTint(baseTint))
                if (tintLayer.alpha > 0.01f) add(HazeTint(tintLayer))
            },
            blurRadius = blurRadius,
            noiseFactor = HazeDefaults.noiseFactor * 0.35f
        )
    }

    val hazeModifier = hazeState?.takeIf { useHaze }?.let { state ->
        Modifier.hazeEffect(state = state, style = hazeStyle)
    } ?: Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = if (strong) 18.dp else 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f),
                clip = false
            )
            .clip(shape)
            .then(hazeModifier)
            .onGloballyPositioned { coords = it }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        VoidGlassBorder,
                        Color.White.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            )
    ) {
        // Legacy wallpaper portal — only when Haze isn't hosting this tree
        if (!useHaze && blurred != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        val b = blurStrength
                        val f = frostAmount
                        val shaderOn = useShader
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
                                    intensity = (if (strong) 0.16f else 0.11f) * b.coerceIn(0.5f, 1.6f),
                                    chromatic = if (strong) 1.6f else 1.0f,
                                    frost = (if (strong) 0.22f else 0.12f) * f,
                                    time = 0f,
                                    bezel = if (strong) 0.09f else 0.06f
                                )
                                AndroidRenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                            } else {
                                null
                            }
                        renderEffect = when {
                            shaderEffect != null && blurEffect != null ->
                                AndroidRenderEffect.createChainEffect(blurEffect, shaderEffect)
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
                    if (wp == null || panel == null || !panel.isAttached || size.minDimension <= 2f) {
                        return@Canvas
                    }
                    val pos = panel.positionInWindow()
                    val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat()
                    val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat()
                    val extraWidthPx = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
                    val pageOffsetPx = wallpaperXOffset.coerceIn(0f, 1f) * extraWidthPx
                    val realX = pageOffsetPx + pos.x
                    val pad = 0.02f
                    val srcX = ((realX - size.width * pad) * scaleX).roundToInt()
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
                    val frost = frostAmount
                    // Light veil only — Haze (or wallpaper sample) supplies the backdrop
                    val veil = if (useHaze) {
                        if (strong) 0.06f else 0.04f
                    } else if (blurred != null) {
                        if (strong) 0.10f else 0.06f
                    } else {
                        if (strong) 0.18f else 0.12f
                    }
                    drawRect(Color.White.copy(alpha = veil * frost))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = veil * 1.2f * frost),
                                Color.White.copy(alpha = veil * 0.5f * frost),
                                Color.Black.copy(alpha = 0.04f * frost)
                            )
                        )
                    )
                    if (tint.alpha > 0.01f && !useHaze) drawRect(tint)
                    drawTopSpecular(strong)
                    if (effectiveSheen) {
                        val bandX = size.width * sheenShift
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.10f),
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
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Black.copy(alpha = 0.20f)
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
        )

        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopSpecular(strong: Boolean) {
    val peak = if (strong) 0.30f else 0.20f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = peak),
                Color.White.copy(alpha = peak * 0.35f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, -size.height * 0.05f),
            radius = size.width * 0.65f
        ),
        topLeft = Offset(-size.width * 0.15f, -size.height * 0.55f),
        size = Size(size.width * 1.3f, size.height * 0.9f)
    )
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
