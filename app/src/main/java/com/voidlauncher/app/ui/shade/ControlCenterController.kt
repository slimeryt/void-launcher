package com.voidlauncher.app.ui.shade

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Window
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Device controls for Control Center. Uses public APIs where possible;
 * falls back to system settings panels when a direct toggle isn't allowed.
 */
class ControlCenterController(
    private val context: Context,
    private val window: Window?
) {
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

    private var torchCameraId: String? = null

    init {
        torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun refresh() {
        rotationLocked = readRotationLocked()
        brightness = readBrightness()
        ringMode = readRingMode()
        volume = readVolume()
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
        open(Settings.ACTION_MANAGE_WRITE_SETTINGS)
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    private fun open(action: String) {
        runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

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
}

enum class RingMode { Normal, Vibrate, Silent }
