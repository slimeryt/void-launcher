package com.voidlauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.glass.LocalBlurredWallpaper
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.LocalWallpaperXOffset
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import dev.liquidglass.compose.GlassHighlight
import dev.liquidglass.compose.GlassRefraction
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.liquidGlass
import dev.liquidglass.compose.liquidGlassProvider
import dev.liquidglass.compose.rememberLiquidGlassProviderState
import kotlin.math.roundToInt

/**
 * Liquid glass panel powered by Abdullajon1881/LiquidGlass (AGSL SDF lens).
 *
 * Backdrop selection:
 * - [sampleWallpaper] true → wallpaper crop (home / Liquid Glass preview)
 * - false + [LocalLiquidGlassProvider] → live UI behind chrome (settings)
 * - false, no shared provider → structured frost plate (still shows rim/lens)
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

    val blurStrength = glass.blurStrength.coerceIn(0f, 1.6f)
    val frostAmount = glass.frostAmount.coerceIn(0f, 1.5f)
    val refractionOn = enableRefraction && glass.refractionEnabled
    val specularOn = enableSheen && glass.sheenEnabled
    val useWallpaper = sampleWallpaper && wallpaper != null
    // Settings chrome stays on a local structured plate (no wallpaper portals).
    // Live UI sampling needs chrome outside the provider subtree — follow-up.

    val shape = remember(cornerRadius) { SmoothCornerShape(radius = cornerRadius) }
    val provider = rememberLiquidGlassProviderState()

    // Independent knobs — wide ranges so Settings sliders are obvious.
    val blurRadius = (32.dp * blurStrength * (if (strong) 1.1f else 1f)).coerceIn(0.dp, 48.dp)
    // Frost → tint wash + slight extra blur (not a hard floor).
    val frostBlurBoost = (12.dp * frostAmount).coerceIn(0.dp, 18.dp)
    val effectiveBlur = blurRadius + frostBlurBoost

    val refraction = if (refractionOn) {
        // Edge band + pixel offset — library defaults are 12/16; push harder so
        // wallpaper detail and chromatic fringe actually read.
        val amount = (if (strong) 28.dp else 22.dp) * (0.65f + 0.35f * blurStrength.coerceIn(0.4f, 1.4f))
        GlassRefraction(
            height = if (strong) 18.dp else 14.dp,
            amount = amount.coerceIn(14.dp, 36.dp)
        )
    } else {
        GlassRefraction.None
    }
    val highlight = if (specularOn) {
        GlassHighlight(
            width = if (strong) 3.dp else 2.2.dp,
            alpha = (if (strong) 0.72f else 0.55f) * frostAmount.coerceIn(0.5f, 1.2f).coerceAtMost(1f),
            lightAngleDegrees = 245f
        )
    } else {
        GlassHighlight.None
    }
    val chromatic = when {
        !refractionOn -> 0f
        useWallpaper -> if (strong) 0.55f else 0.4f
        else -> 0.28f // plate path: fringe still readable when toggling refraction
    }
    val style = GlassStyle(
        shape = GlassShape.RoundedRectangle(cornerRadius),
        blurRadius = effectiveBlur,
        refraction = refraction,
        saturation = if (useWallpaper) 1.55f else 1.2f,
        tint = when {
            tint.alpha > 0.01f -> tint
            frostAmount > 0.05f -> Color.White.copy(
                alpha = (0.04f + 0.14f * frostAmount).coerceIn(0.04f, 0.28f)
            )
            else -> Color.Unspecified
        },
        highlight = highlight,
        noiseAlpha = 0.018f,
        chromaticAberration = chromatic,
        isInteractive = false,
        fallbackScrim = Color(0xFF2C2C2E).copy(alpha = 0.72f)
    )

    var panelPos by remember { mutableStateOf<Offset?>(null) }

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
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val p = coords.positionInWindow()
                    panelPos = Offset(p.x, p.y)
                }
            }
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
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .liquidGlassProvider(provider)
        ) {
            if (size.minDimension <= 2f) return@Canvas
            if (useWallpaper) {
                val wp = wallpaper ?: return@Canvas
                val pos = panelPos ?: return@Canvas
                val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat()
                val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat()
                val extraWidthPx = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
                val pageOffsetPx = wallpaperXOffset.coerceIn(0f, 1f) * extraWidthPx
                val realX = pageOffsetPx + pos.x
                val pad = 0.12f
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
                // Structured plate so edge refraction / chromatic still read
                // without wallpaper portals.
                drawRect(Color(0xFF1C1C1E))
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.07f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                drawRect(Color.White.copy(alpha = 0.06f * frostAmount.coerceIn(0.3f, 1.5f)))
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .liquidGlass(provider, style)
                .drawBehind {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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
