package com.voidlauncher.app.glass

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

data class BlurredWallpaper(
    val image: ImageBitmap,
    /** Pixel size of the bitmap (blur buffer). */
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    /** Logical screen size the bitmap is mapped to. */
    val screenWidth: Int,
    val screenHeight: Int
)

val LocalBlurredWallpaper = staticCompositionLocalOf<BlurredWallpaper?> { null }

class WallpaperBlurController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _wallpaper = MutableStateFlow<BlurredWallpaper?>(null)
    val wallpaper: StateFlow<BlurredWallpaper?> = _wallpaper.asStateFlow()

    private var loadJob: Job? = null
    private var receiverRegistered = false

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refresh()
        }
    }

    fun start() {
        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
            ContextCompat.registerReceiver(
                context,
                wallpaperReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        refresh()
    }

    fun stop() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(wallpaperReceiver) }
            receiverRegistered = false
        }
        loadJob?.cancel()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val result = withContext(Dispatchers.Default) {
                captureAndBlur()
            }
            if (result != null) {
                _wallpaper.value = result
            }
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun captureAndBlur(): BlurredWallpaper? {
        val (screenW, screenH) = screenSize()
        if (screenW <= 0 || screenH <= 0) return null

        val drawable = loadWallpaperDrawable() ?: return null
        val source = drawableToBitmap(drawable, screenW, screenH) ?: return null

        // Work at ~1/2–1/3 res for speed; soft upscale reads as deeper frost.
        val targetW = (screenW / 2.5f).roundToInt().coerceIn(240, 900)
        val targetH = (screenH * (targetW.toFloat() / screenW)).roundToInt().coerceAtLeast(240)
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled !== source) source.recycle()

        val vibrancy = applyVibrancy(scaled)
        if (vibrancy !== scaled) scaled.recycle()

        // Stronger liquid-glass blur so panels read frosted, not sharp wallpaper crops
        val blurRadius = (minOf(targetW, targetH) * 0.22f).roundToInt().coerceIn(16, 120)
        val blurred = StackBlur.blur(vibrancy, radius = blurRadius)
        if (blurred !== vibrancy) vibrancy.recycle()

        return BlurredWallpaper(
            image = blurred.asImageBitmap(),
            bitmapWidth = blurred.width,
            bitmapHeight = blurred.height,
            screenWidth = screenW,
            screenHeight = screenH
        )
    }

    private fun loadWallpaperDrawable(): Drawable? {
        return runCatching {
            val wm = WallpaperManager.getInstance(context)
            wm.drawable ?: wm.peekDrawable() ?: wm.builtInDrawable
        }.getOrNull()
    }

    private fun drawableToBitmap(drawable: Drawable, screenW: Int, screenH: Int): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val src = drawable.bitmap
            // Center-crop to screen aspect
            return centerCrop(src, screenW, screenH)
        }

        val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: screenW
        val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: screenH
        val bmp = Bitmap.createBitmap(
            max(intrinsicW, 1),
            max(intrinsicH, 1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return centerCrop(bmp, screenW, screenH).also {
            if (it !== bmp) bmp.recycle()
        }
    }

    private fun centerCrop(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val scale = max(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
        val scaledW = (src.width * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (src.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        val x = ((scaledW - targetW) / 2).coerceAtLeast(0)
        val y = ((scaledH - targetH) / 2).coerceAtLeast(0)
        val w = targetW.coerceAtMost(scaledW)
        val h = targetH.coerceAtMost(scaledH)
        val cropped = Bitmap.createBitmap(scaled, x, y, w, h)
        if (scaled !== src && scaled !== cropped) scaled.recycle()
        return cropped
    }

    /** Slight saturation boost so glass feels “alive” over muted wallpapers. */
    private fun applyVibrancy(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val matrix = ColorMatrix().apply {
            setSaturation(1.25f)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        // Soft frost baked into blur buffer (helps glass read even if sample is sharp)
        canvas.drawRect(
            Rect(0, 0, src.width, src.height),
            Paint().apply {
                color = 0x28FFFFFF
            }
        )
        return out
    }
}
