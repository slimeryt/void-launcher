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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.voidlauncher.app.glass.LocalGlassSettings
import com.voidlauncher.app.glass.LocalLayerBackdrop
import com.voidlauncher.app.glass.applyPolarGlass
import com.voidlauncher.app.ui.theme.VoidGlassBorder
import kotlin.math.roundToInt

/**
 * Optical liquid glass via Kyant Backdrop (AGSL refraction / blur / vibrancy)
 * when a [LocalLayerBackdrop] is provided. Falls back to a frosted plate + stroke
 * when no backdrop layer is available.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    strong: Boolean = false,
    enableSheen: Boolean = true,
    enableRefraction: Boolean = true,
    sampleWallpaper: Boolean = true,
    /**
     * When set, overrides [LocalGlassSettings.blurStrength] (e.g. 0.5f = fixed 50% blur
     * for the App Icons editor panel).
     */
    blurStrengthOverride: Float? = null,
    tint: Color = Color.Transparent,
    /** 0 = circular corners, 1 = max continuous smoothing. */
    cornerSmoothing: Float = DefaultCornerSmoothing,
    /** Stadium ends (50% radius) — use for 1×2 / 2×1 CC tiles. */
    capsule: Boolean = false,
    /** Keep the Liquid Glass rim even when refraction/sheen are off (CC frost tiles). */
    liquidStroke: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassSettings.current
    val backdrop = LocalLayerBackdrop.current
    val blurStrength = (blurStrengthOverride ?: glass.blurStrength).coerceIn(0f, 1.6f)
    val refractionOn = enableRefraction && glass.refractionEnabled
    val specularOn = enableSheen && glass.sheenEnabled
    val smoothing = cornerSmoothing.coerceIn(0f, 1f)

    // Kyant lens SDF supports CornerBasedShape — not SmoothCornerShape.
    val glassShape: () -> Shape = remember(capsule, cornerRadius) {
        {
            if (capsule) RoundedCornerShape(percent = 50)
            else RoundedCornerShape(cornerRadius)
        }
    }
    val clipShape = remember(capsule, cornerRadius, smoothing) {
        if (capsule) SmoothCornerShape(percent = 50, smoothing = smoothing)
        else SmoothCornerShape(radius = cornerRadius, smoothing = smoothing)
    }

    if (backdrop != null) {
        val blurRadiusDp = when {
            blurStrengthOverride != null ->
                (28f * blurStrength).coerceIn(0f, 40f)
            else ->
                glass.blurRadiusDp.coerceIn(0f, 40f)
        }
        val panelGlass = glass.copy(
            blurRadiusDp = blurRadiusDp,
            blurEnabled = glass.blurEnabled && (blurStrengthOverride == null || blurStrength > 0.01f),
            refractionEnabled = refractionOn
        )
        val frostScale = glass.frostAmount.coerceIn(0f, 1.5f)
        val surfaceAlpha = when {
            tint.alpha > 0.01f -> 0f
            sampleWallpaper -> (if (strong) 0.08f else 0.05f) * frostScale
            else -> (if (strong) 0.12f else 0.08f) * frostScale
        }

        // Kyant caches the effects lambda until the modifier node is replaced.
        key(
            panelGlass,
            surfaceAlpha,
            specularOn,
            liquidStroke,
            tint,
            strong
        ) {
            Box(
                modifier = modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = glassShape,
                    effects = {
                        applyPolarGlass(
                            glass = panelGlass,
                            strong = strong,
                            depthEffect = strong
                        )
                    },
                    highlight = if (specularOn || liquidStroke) {
                        { Highlight.Default }
                    } else {
                        null
                    },
                    onDrawSurface = {
                        if (surfaceAlpha > 0.01f) {
                            drawRect(Color.White.copy(alpha = surfaceAlpha))
                        }
                        if (tint.alpha > 0.01f) drawRect(tint)
                    }
                )
            ) {
                content()
            }
        }
        return
    }

    // No LayerBackdrop yet — lightweight frosted plate so layout still works.
    Box(
        modifier = modifier
            .then(
                if (liquidStroke) {
                    Modifier.liquidGlassStroke(shape = clipShape, strong = strong)
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
                        shape = clipShape
                    )
                }
            )
            .clip(clipShape)
            .drawBehind {
                val base = if (sampleWallpaper) {
                    Color.White.copy(alpha = if (strong) 0.14f else 0.1f)
                } else {
                    Color.White.copy(alpha = if (strong) 0.12f else 0.08f)
                }
                drawRect(base)
                if (tint.alpha > 0.01f) drawRect(tint)
            }
    ) {
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
