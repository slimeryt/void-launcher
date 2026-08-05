package com.voidlauncher.app.glass

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Wallpaper bitmap access on modern Android is restricted to privileged system apps
 * (WallpaperManager.getDrawable() throws SecurityException on API 30+ without this).
 * Polar is sideloaded/OTA-only, not Play-distributed, so requesting All Files Access
 * is the pragmatic way to keep Liquid Glass tracking the real live wallpaper.
 */
object StoragePermission {

    fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Legacy runtime permission (API 26-29) — no-op on API 30+, which uses [requestIntent] instead. */
    fun legacyRuntimePermission(): String = android.Manifest.permission.READ_EXTERNAL_STORAGE

    fun needsLegacyRuntimeRequest(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    /** Settings screen to grant All Files Access (API 30+). */
    fun requestIntent(context: Context): Intent {
        val perAppIntent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        return if (perAppIntent.resolveActivity(context.packageManager) != null) {
            perAppIntent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }
}
