package com.voidlauncher.app.ui.shade

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.Window
import androidx.core.content.ContextCompat
import com.voidlauncher.app.notifications.PolarNotificationListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class NowPlaying(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val hasSession: Boolean = false,
    val artwork: Bitmap? = null
)

/**
 * Device controls for Control Center. Uses public APIs where possible;
 * requests the matching permission / settings panel when a toggle is blocked.
 */
class ControlCenterController(
    private val context: Context,
    private val window: Window?
) {
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    var flashlightOn by mutableStateOf(false)
        private set
    var rotationLocked by mutableStateOf(readRotationLocked())
        private set
    var brightness by mutableFloatStateOf(readBrightness())
        private set
    var ringMode by mutableStateOf(readRingMode())
        private set
    var volume by mutableFloatStateOf(readVolume())
        private set
    var wifiEnabled by mutableStateOf(false)
        private set
    var wifiSsid by mutableStateOf("")
        private set
    var bluetoothEnabled by mutableStateOf(false)
        private set
    var bluetoothName by mutableStateOf("")
        private set
    var mobileDataEnabled by mutableStateOf(false)
        private set
    var mobileCarrier by mutableStateOf("")
        private set
    var dndOn by mutableStateOf(false)
        private set
    var nowPlaying by mutableStateOf(NowPlaying())
        private set

    private var torchCameraId: String? = null
    private var mediaController: MediaController? = null
    private var listening = false

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishNowPlaying()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishNowPlaying()
        override fun onSessionDestroyed() {
            mediaController?.unregisterCallback(this)
            mediaController = null
            nowPlaying = NowPlaying()
        }
    }

    private val sessionsChanged =
        MediaSessionManager.OnActiveSessionsChangedListener { bindMedia(it) }

    init {
        torchCameraId = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        refresh()
    }

    fun startListening() {
        if (listening) return
        listening = true
        val listener = listenerComponent()
        runCatching {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChanged,
                listener,
                mainHandler
            )
            bindMedia(mediaSessionManager.getActiveSessions(listener))
        }
        refresh()
    }

    fun stopListening() {
        if (!listening) return
        listening = false
        runCatching {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChanged)
        }
        mediaController?.unregisterCallback(mediaCallback)
        mediaController = null
    }

    fun refresh() {
        rotationLocked = readRotationLocked()
        brightness = readBrightness()
        ringMode = readRingMode()
        volume = readVolume()
        wifiEnabled = wifiManager.isWifiEnabled
        wifiSsid = readWifiSsid()
        bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        bluetoothName = readBluetoothName()
        mobileDataEnabled = readMobileData()
        mobileCarrier = telephonyManager?.networkOperatorName.orEmpty()
        dndOn = readDnd()
        publishNowPlaying()
    }

    fun applyBrightness(fraction: Float) {
        val v = fraction.coerceIn(0f, 1f)
        brightness = v
        val intBright = (v * 255f).toInt().coerceIn(1, 255)
        window?.attributes = window?.attributes?.apply {
            screenBrightness = v.coerceIn(0.01f, 1f)
        }
        if (Settings.System.canWrite(context)) {
            runCatching {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    intBright
                )
            }
        }
    }

    fun applyVolume(fraction: Float) {
        val v = fraction.coerceIn(0f, 1f)
        volume = v
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val index = (v * max).toInt().coerceIn(0, max)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        }
    }

    fun toggleFlashlight(): Boolean {
        val id = torchCameraId ?: return false
        return runCatching {
            val next = !flashlightOn
            cameraManager.setTorchMode(id, next)
            flashlightOn = next
            true
        }.getOrDefault(false)
    }

    fun toggleRotationLock(): Boolean {
        if (!Settings.System.canWrite(context)) {
            openWriteSettings()
            return false
        }
        return runCatching {
            val next = !rotationLocked
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (next) 0 else 1
            )
            rotationLocked = next
            true
        }.getOrDefault(false)
    }

    fun cycleRingMode() {
        val next = when (ringMode) {
            RingMode.Normal -> RingMode.Vibrate
            RingMode.Vibrate -> RingMode.Silent
            RingMode.Silent -> RingMode.Normal
        }
        val amMode = when (next) {
            RingMode.Normal -> AudioManager.RINGER_MODE_NORMAL
            RingMode.Vibrate -> AudioManager.RINGER_MODE_VIBRATE
            RingMode.Silent -> AudioManager.RINGER_MODE_SILENT
        }
        runCatching { audioManager.ringerMode = amMode }
        ringMode = readRingMode()
    }

    fun toggleWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openPanel(Settings.Panel.ACTION_WIFI)
            return
        }
        val ok = runCatching { @Suppress("DEPRECATION") wifiManager.setWifiEnabled(!wifiEnabled) }
            .getOrDefault(false)
        if (!ok) open(Settings.ACTION_WIFI_SETTINGS)
        refresh()
    }

    fun toggleBluetooth(requestConnect: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            requestConnect()
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null) {
            open(Settings.ACTION_BLUETOOTH_SETTINGS)
            return
        }
        val toggled = runCatching {
            @Suppress("DEPRECATION")
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
            true
        }.getOrDefault(false)
        if (!toggled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                openPanel(Settings.Panel.ACTION_BLUETOOTH)
            } else {
                open(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
        }
        mainHandler.postDelayed({ refresh() }, 400)
    }

    fun toggleMobileData(requestPhone: () -> Unit) {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            requestPhone()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openPanel(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            open(Settings.ACTION_DATA_ROAMING_SETTINGS)
        }
    }

    fun toggleDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            return
        }
        val next = if (dndOn) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            NotificationManager.INTERRUPTION_FILTER_NONE
        }
        runCatching { notificationManager.setInterruptionFilter(next) }
        dndOn = readDnd()
    }

    fun playPause() {
        val controls = mediaController?.transportControls ?: return
        if (nowPlaying.isPlaying) controls.pause() else controls.play()
    }

    fun skipNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun openNowPlayingApp() {
        val session = mediaController?.sessionActivity
        if (session != null) {
            runCatching { session.send() }
            return
        }
        val pkg = mediaController?.packageName ?: return
        context.packageManager.getLaunchIntentForPackage(pkg)?.let { openIntent(it) }
    }

    fun openWifi() = open(Settings.ACTION_WIFI_SETTINGS)
    fun openBluetooth() = open(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openAirplane() = open(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    fun openSound() = open(Settings.ACTION_SOUND_SETTINGS)
    fun openDisplay() = open(Settings.ACTION_DISPLAY_SETTINGS)
    fun openNetwork() = open(Settings.ACTION_WIRELESS_SETTINGS)
    fun openBattery() = open(
        if (Build.VERSION.SDK_INT >= 22) Settings.ACTION_BATTERY_SAVER_SETTINGS
        else Settings.ACTION_SETTINGS
    )

    fun openWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
        if (!openIntent(intent)) open(Settings.ACTION_MANAGE_WRITE_SETTINGS)
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    private fun bindMedia(sessions: List<MediaController>?) {
        mediaController?.unregisterCallback(mediaCallback)
        mediaController = sessions?.firstOrNull()
        mediaController?.registerCallback(mediaCallback, mainHandler)
        publishNowPlaying()
    }

    private fun publishNowPlaying() {
        val controller = mediaController
        if (controller == null) {
            nowPlaying = NowPlaying()
            return
        }
        val meta = controller.metadata
        val state = controller.playbackState
        nowPlaying = NowPlaying(
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty() },
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty() },
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            hasSession = true,
            artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        )
    }

    private fun listenerComponent(): ComponentName =
        ComponentName(context, PolarNotificationListener::class.java)

    private fun openPanel(action: String) {
        if (!open(action)) open(Settings.ACTION_SETTINGS)
    }

    private fun open(action: String): Boolean {
        return openIntent(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openIntent(intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    private fun readBrightness(): Float {
        val system = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        val win = window?.attributes?.screenBrightness
            ?.takeIf { it in 0f..1f }
        return win ?: (system / 255f).coerceIn(0f, 1f)
    }

    private fun readRotationLocked(): Boolean {
        val auto = runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            )
        }.getOrDefault(1)
        return auto == 0
    }

    private fun readRingMode(): RingMode = when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> RingMode.Vibrate
        AudioManager.RINGER_MODE_SILENT -> RingMode.Silent
        else -> RingMode.Normal
    }

    private fun readVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (cur.toFloat() / max).coerceIn(0f, 1f)
    }

    private fun readWifiSsid(): String {
        if (!wifiManager.isWifiEnabled) return ""
        val info = runCatching { wifiManager.connectionInfo }.getOrNull() ?: return ""
        val raw = info.ssid?.trim('"').orEmpty()
        return if (raw.isBlank() || raw == "<unknown ssid>") "" else raw
    }

    private fun readBluetoothName(): String {
        if (bluetoothAdapter?.isEnabled != true) return ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            return ""
        }
        return runCatching { bluetoothAdapter.name }.getOrNull().orEmpty()
    }

    private fun readMobileData(): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        ) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun readDnd(): Boolean {
        val filter = notificationManager.currentInterruptionFilter
        return filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
}

enum class RingMode { Normal, Vibrate, Silent }
