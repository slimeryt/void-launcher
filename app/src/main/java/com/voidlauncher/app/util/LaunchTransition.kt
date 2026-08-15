package com.voidlauncher.app.util

import android.app.ActivityOptions
import android.graphics.Rect
import android.os.Build
import android.view.View
import java.lang.ref.WeakReference

/**
 * Bridges the icon tap position (known only to the Compose UI) to [com.voidlauncher.app.data.AppRepository],
 * which owns the actual `startActivity` call but only has an application Context.
 */
object PendingLaunchBounds {
    @Volatile
    var rect: Rect? = null

    /** Tighter icon-glyph box when [AppIcon] measured the image itself. */
    @Volatile
    var iconRect: Rect? = null

    private val iconByKey = java.util.concurrent.ConcurrentHashMap<String, Rect>()

    fun rememberIcon(key: String, bounds: Rect) {
        if (bounds.width() > 0 && bounds.height() > 0) {
            iconByKey[key] = Rect(bounds)
        }
    }

    fun peek(key: String? = null): Rect? =
        key?.let { iconByKey[it] }?.takeIf { it.width() > 0 && it.height() > 0 }
            ?: iconRect?.takeIf { it.width() > 0 && it.height() > 0 }
            ?: rect?.takeIf { it.width() > 0 && it.height() > 0 }

    fun take(key: String? = null): Rect? {
        val r = peek(key)
        rect = null
        iconRect = null
        return r
    }

    fun copy(key: String? = null): Rect? = peek(key)?.let { Rect(it) }
}

/** The launcher's root window, needed as the `source` view for scale-up / clip-reveal. */
object LauncherWindow {
    private var ref: WeakReference<View>? = null

    var decorView: View?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}

/**
 * System window options for opening any app. Clip-reveal is the Polar look; some OEM
 * launchers/skins ignore it, in which case we fall back to scale-up.
 */
object PolarActivityOptions {
    fun bundle(bounds: Rect?): android.os.Bundle? {
        val source = LauncherWindow.decorView ?: return null
        val box = bounds ?: return null
        if (box.width() <= 0 || box.height() <= 0) return null
        val x = box.left.coerceAtLeast(0)
        val y = box.top.coerceAtLeast(0)
        val w = box.width()
        val h = box.height()
        if (Build.VERSION.SDK_INT >= 23) {
            val clip = runCatching {
                ActivityOptions.makeClipRevealAnimation(source, x, y, w, h).toBundle()
            }.getOrNull()
            if (clip != null) return clip
        }
        return runCatching {
            ActivityOptions.makeScaleUpAnimation(source, x, y, w, h).toBundle()
        }.getOrNull()
    }
}
