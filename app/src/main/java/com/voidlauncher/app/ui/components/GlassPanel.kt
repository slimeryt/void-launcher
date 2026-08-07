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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
 * Liquid glass via Abdullajon1881/LiquidGlass.
 *
 * The backdrop [liquidGlassProvider] is intentionally *larger* than the glass
 * silhouette so edge refraction can sample content outside the panel — same-size
 * provider+glass only magnifies rim pixels and reads as "no lens."
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

    val blurStrength = glass.blurStrength.coerceIn(0f, 1.6f)
    val frostAmount = glass.frostAmount.coerceIn(0f, 1.5f)
    val refractionOn = enableRefraction && glass.refractionEnabled
    val specularOn = enableSheen && glass.sheenEnabled
    val useWallpaper = sampleWallpaper && wallpaper != null

    val shape = remember(cornerRadius) { SmoothCornerShape(radius = cornerRadius) }
    val provider = rememberLiquidGlassProviderState()

    // Keep blur modest — heavy Gaussian erases the frequencies refraction needs.
    val blurRadius = if (blurStrength <= 0.01f) {
        0.dp
    } else {
        (12.dp * blurStrength * (if (strong) 1.05f else 1f)).coerceIn(0.dp, 20.dp)
    }
    val frostTint = (0.02f + 0.16f * frostAmount).coerceIn(0f, 0.3f)

    val refraction = if (refractionOn) {
        GlassRefraction(
            height = if (strong) 48.dp else 40.dp,
            amount = if (strong) 56.dp else 48.dp
        )
    } else {
        GlassRefraction.None
    }
    val highlight = if (specularOn) {
        GlassHighlight(
            width = if (strong) 3.dp else 2.4.dp,
            alpha = if (strong) 0.78f else 0.62f,
            lightAngleDegrees = 245f
        )
    } else {
        GlassHighlight.None
    }
    val chromatic = when {
        !refractionOn -> 0f
        useWallpaper -> if (strong) 0.9f else 0.75f
        else -> 0.5f
    }
    val style = GlassStyle(
        shape = GlassShape.RoundedRectangle(cornerRadius),
        blurRadius = blurRadius,
        refraction = refraction,
        saturation = if (useWallpaper) 1.65f else 1.25f,
        tint = when {
            tint.alpha > 0.01f -> tint
            frostAmount > 0.02f -> Color.White.copy(alpha = frostTint)
            else -> Color.Unspecified
        },
        highlight = highlight,
        noiseAlpha = 0.02f,
        chromaticAberration = chromatic,
        isInteractive = false,
        fallbackScrim = Color(0xFF2C2C2E).copy(alpha = 0.72f)
    )

    // Extra margin around the glass so the lens can pull outside detail.
    val providerPad = if (refractionOn) 72.dp else 24.dp

    var panelPos by remember { mutableStateOf<Offset?>(null) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

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
                    panelSize = coords.size
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
        val providerDpSize = with(density) {
            if (panelSize.width > 0 && panelSize.height > 0) {
                DpSize(
                    panelSize.width.toDp() + providerPad * 2,
                    panelSize.height.toDp() + providerPad * 2
                )
            } else {
                DpSize(0.dp, 0.dp)
            }
        }

        if (providerDpSize.width > 0.dp && providerDpSize.height > 0.dp) {
            Box(
                modifier = Modifier
                    .requiredSize(providerDpSize)
                    .align(Alignment.Center)
                    .liquidGlassProvider(provider)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.minDimension <= 2f) return@Canvas
                    if (useWallpaper) {
                        val wp = wallpaper ?: return@Canvas
                        val pos = panelPos ?: return@Canvas
                        val padPx = with(density) { providerPad.toPx() }
                        val scaleX = wp.bitmapWidth.toFloat() / wp.fullWidthPx.toFloat()
                        val scaleY = wp.bitmapHeight.toFloat() / wp.fullHeightPx.toFloat()
                        val extraWidthPx = (wp.fullWidthPx - wp.screenWidth).coerceAtLeast(0)
                        val pageOffsetPx = wallpaperXOffset.coerceIn(0f, 1f) * extraWidthPx
                        // Provider is centered on the panel; its top-left is panel - pad.
                        val realX = pageOffsetPx + pos.x - padPx
                        val realY = pos.y - padPx
                        val srcX = (realX * scaleX).roundToInt()
                            .coerceIn(0, (wp.bitmapWidth - 1).coerceAtLeast(0))
                        val srcY = (realY * scaleY).roundToInt()
                            .coerceIn(0, (wp.bitmapHeight - 1).coerceAtLeast(0))
                        val srcW = (size.width * scaleX).roundToInt().coerceAtLeast(1)
                            .coerceAtMost((wp.bitmapWidth - srcX).coerceAtLeast(1))
                        val srcH = (size.height * scaleY).roundToInt().coerceAtLeast(1)
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
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent,
                                    Color(0xFF4A90D9).copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.1f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                    }
                }
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
