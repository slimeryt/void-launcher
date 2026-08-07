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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
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
    /** Pixel size of the bitmap (blur buffer). May be wider than one screen if the
     *  system wallpaper spans multiple home pages (parallax). */
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    /** One home page's logical screen size. */
    val screenWidth: Int,
    val screenHeight: Int,
    /** True pixel size of the source wallpaper before downscaling — needed to convert
     *  a page-scroll fraction into an offset within [bitmapWidth]. Equals screenWidth/
     *  screenHeight for a single-page (non-parallax) wallpaper. */
    val fullWidthPx: Int,
    val fullHeightPx: Int
)

val LocalBlurredWallpaper = staticCompositionLocalOf<BlurredWallpaper?> { null }

/**
 * Pager → wallpaper parallax (0..1). Held in a mutable state object so glass can
 * read [offset] inside draw/graphicsLayer without recomposing the whole panel
 * every scroll frame (that lag was the dock delay).
 */
class WallpaperScrollState {
    var offset by mutableFloatStateOf(0.5f)
}

val LocalWallpaperScrollState = staticCompositionLocalOf { WallpaperScrollState() }

/** @deprecated Prefer [LocalWallpaperScrollState] for draw-phase reads. */
val LocalWallpaperXOffset = compositionLocalOf { 0.5f }

class WallpaperBlurController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _wallpaper = MutableStateFlow<BlurredWallpaper?>(null)
    val wallpaper: StateFlow<BlurredWallpaper?> = _wallpaper.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    /** Whether we're actually allowed to read the real wallpaper bitmap right now. */
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

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
        if (!StoragePermission.isGranted(context)) {
            // Don't bother calling an API we know will throw — just report the gap.
            _hasAccess.value = false
            return
        }
        loadJob = scope.launch {
            val result = withContext(Dispatchers.Default) {
                captureAndBlur()
            }
            if (result != null) {
                _hasAccess.value = true
                _wallpaper.value = result
            } else {
                _hasAccess.value = false
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
        // Height-fit, preserve natural width. Do NOT upscale to desiredMinimumWidth —
        // that over-zoomed the buffer so glass parallax raced ahead of the real wallpaper.
        val source = drawableToScreenHeightBitmap(drawable, screenW, screenH) ?: return null
        val fullWidthPx = source.width
        val fullHeightPx = source.height

        val targetH = (fullHeightPx / 1.5f).roundToInt().coerceIn(400, 2400)
        val targetW = (fullWidthPx * (targetH.toFloat() / fullHeightPx)).roundToInt().coerceAtLeast(400)
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled !== source) source.recycle()

        val vibrancy = applyVibrancy(scaled)
        if (vibrancy !== scaled) scaled.recycle()

        return BlurredWallpaper(
            image = vibrancy.asImageBitmap(),
            bitmapWidth = vibrancy.width,
            bitmapHeight = vibrancy.height,
            screenWidth = screenW,
            screenHeight = screenH,
            fullWidthPx = fullWidthPx,
            fullHeightPx = fullHeightPx
        )
    }

    private fun loadWallpaperDrawable(): Drawable? {
        return runCatching {
            val wm = WallpaperManager.getInstance(context)
            wm.drawable ?: wm.peekDrawable() ?: wm.builtInDrawable
        }.getOrNull()
    }

    /**
     * Scale so height == screen height; keep aspect (parallax width intact when the
     * drawable is already multi-screen wide). Never invent extra width.
     */
    private fun drawableToScreenHeightBitmap(
        drawable: Drawable,
        screenW: Int,
        screenH: Int
    ): Bitmap? {
        val src = if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            drawable.bitmap
        } else {
            val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: screenW
            val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: screenH
            val bmp = Bitmap.createBitmap(max(intrinsicW, 1), max(intrinsicH, 1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }

        val scale = screenH.toFloat() / src.height.toFloat().coerceAtLeast(1f)
        val scaledW = (src.width * scale).roundToInt().coerceAtLeast(1)
        val scaledH = screenH
        if (scaledW == src.width && scaledH == src.height) return src
        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    /** Slight saturation boost so glass feels "alive" over muted wallpapers — no tint/wash here;
     *  frost is entirely GlassPanel's job, driven by the live frostAmount setting. */
    private fun applyVibrancy(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val matrix = ColorMatrix().apply {
            setSaturation(1.1f)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
