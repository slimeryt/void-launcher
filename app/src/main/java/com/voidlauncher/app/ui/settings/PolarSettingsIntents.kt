package com.voidlauncher.app.ui.settings

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import com.voidlauncher.app.MainActivity
import com.voidlauncher.app.notifications.NotificationMirror

object PolarSettingsIntents {
    fun open(context: Context, action: String, extra: Uri? = null) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (extra != null) intent.data = extra
        runCatching { context.startActivity(intent) }
    }

    fun appDetails(context: Context) {
        open(context, ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }

    fun polarHome(context: Context, extra: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            extra?.let { putExtra(it, true) }
        }
        context.startActivity(intent)
    }

    fun wifiLabel(context: Context): String {
        val info = runCatching {
            context.applicationContext.getSystemService(WifiManager::class.java)
                ?.connectionInfo?.ssid
        }.getOrNull()?.trim('"').orEmpty()
        return when {
            info.isBlank() || info == "<unknown ssid>" || info == "0x" -> "Networks"
            else -> info
        }
    }

    fun bluetoothLabel(context: Context): String {
        return runCatching {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            when {
                adapter == null -> "Bluetooth"
                !adapter.isEnabled -> "Off"
                adapter.name.isNullOrBlank() -> "On"
                else -> adapter.name
            }
        }.getOrDefault("Bluetooth")
    }

    fun notificationsLabel(context: Context): String =
        if (NotificationMirror.isAccessGranted(context)) "Polar Notification Center" else "Setup required"

    fun modelLabel(): String = listOf(Build.MANUFACTURER, Build.MODEL)
        .map { it.orEmpty().trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .joinToString(" ")
        .ifBlank { "Android device" }
}
