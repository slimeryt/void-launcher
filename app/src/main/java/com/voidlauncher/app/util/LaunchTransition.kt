package com.voidlauncher.app.util

import android.graphics.Rect
import android.view.View
import java.lang.ref.WeakReference

/**
 * Bridges the icon tap position (known only to the Compose UI) to [AppRepository],
 * which owns the actual `startActivity` call but only has an application [Context].
 * `AppIcon` stashes its own on-screen bounds right before invoking its click callback;
 * the repository consumes (and clears) them when building the launch `ActivityOptions`.
 */
object PendingLaunchBounds {
    @Volatile
    var rect: Rect? = null
}

/** The launcher's root window, needed as the `source` view for scale-up launch animations. */
object LauncherWindow {
    private var ref: WeakReference<View>? = null

    var decorView: View?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
