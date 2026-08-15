package com.voidlauncher.app.ui.statusbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

enum class BatteryGlyph {
    Idle,
    Charging,
    Hold,
    LowPower,
    Critical
}

class PolarStatusBarController(private val context: Context) {
    private val app = context.applicationContext
    private val wifiManager =
        app.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val powerManager =
        app.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val telephonyManager =
        app.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val mainHandler = Handler(Looper.getMainLooper())

    var batteryPercent by mutableIntStateOf(100)
        private set
    var batteryGlyph by mutableStateOf(BatteryGlyph.Idle)
        private set
    var wifiConnected by mutableStateOf(false)
        private set
    var wifiBars by mutableIntStateOf(0)
        private set
    var cellularBars by mutableIntStateOf(0)
        private set
    var airplane by mutableStateOf(false)
        private set

    private var listening = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> applyBattery(intent)
                else -> refreshRadios()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshRadios()
        override fun onLost(network: Network) = refreshRadios()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
            refreshRadios()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!listening) return
            refreshRadios()
            mainHandler.postDelayed(this, 8_000L)
        }
    }

    fun start() {
        if (listening) return
        listening = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        runCatching {
            ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback, mainHandler)
        }
        val sticky = runCatching {
            app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        if (sticky != null) applyBattery(sticky) else refreshRadios()
        refreshRadios()
        mainHandler.postDelayed(tick, 8_000L)
    }

    fun stop() {
        if (!listening) return
        listening = false
        mainHandler.removeCallbacks(tick)
        runCatching { app.unregisterReceiver(receiver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    private fun applyBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = if (level < 0) batteryPercent else ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val full = status == BatteryManager.BATTERY_STATUS_FULL || percent >= 100
        val hold = plugged && !charging && !full
        val lowPower = powerManager.isPowerSaveMode
        batteryPercent = percent
        batteryGlyph = when {
            lowPower -> BatteryGlyph.LowPower
            charging && !full -> BatteryGlyph.Charging
            hold -> BatteryGlyph.Hold
            percent <= 20 && !plugged -> BatteryGlyph.Critical
            else -> BatteryGlyph.Idle
        }
    }

    private fun refreshRadios() {
        airplane = Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val wifiOn = wifiManager.isWifiEnabled
        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        wifiConnected = wifiOn && hasWifi
        wifiBars = if (wifiConnected) readWifiBars() else 0
        cellularBars = if (airplane) 0 else readCellularBars()
        if (!listening) return
        // Keep Low Power in sync even if battery sticky didn't re-fire.
        if (powerManager.isPowerSaveMode && batteryGlyph != BatteryGlyph.LowPower) {
            batteryGlyph = BatteryGlyph.LowPower
        } else if (!powerManager.isPowerSaveMode && batteryGlyph == BatteryGlyph.LowPower) {
            val sticky = runCatching {
                app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            if (sticky != null) applyBattery(sticky)
        }
    }

    private fun readWifiBars(): Int {
        val rssi = runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                (caps?.transportInfo as? android.net.wifi.WifiInfo)?.rssi
            } else {
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo?.rssi
            }
        }.getOrNull() ?: return 3
        @Suppress("DEPRECATION")
        val raw = WifiManager.calculateSignalLevel(rssi, 4)
        return raw.coerceIn(0, 4).let { if (it <= 0) 0 else ((it * 3) / 4).coerceIn(1, 3) }
    }

    private fun readCellularBars(): Int {
        val strength = runCatching { telephonyManager?.signalStrength }.getOrNull() ?: return 3
        return if (Build.VERSION.SDK_INT >= 28) {
            strength.level.coerceIn(0, 4)
        } else {
            3
        }
    }
}
