package com.voidlauncher.app.ui.settings

import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.notifications.NotificationMirror
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.viewmodel.LauncherUiState

@Composable
internal fun rememberSettingsTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return tick
}

@Composable
internal fun SettingsPageTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
internal fun NotificationsSettingsPage() {
    val context = LocalContext.current
    rememberSettingsTick()
    val granted = NotificationMirror.isAccessGranted(context)
    SettingsPageTitle("Notifications")
    SettingsGroup(label = "Polar") {
        NavRow(
            title = "Notification Center",
            subtitle = if (granted) "Access granted" else "Required for Polar NC",
            onClick = { NotificationMirror.openAccessSettings(context) },
            showDivider = true
        )
        NavRow(
            title = "App notifications",
            subtitle = "System notification settings",
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            },
            showDivider = false
        )
    }
    Text(
        text = "Pull down from the left of the Polar status bar for Notification Center.",
        style = MaterialTheme.typography.bodyMedium,
        color = VoidMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
internal fun SoundsHapticsPage(
    state: LauncherUiState,
    onHapticChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    SettingsPageTitle("Sounds & Haptics")
    SettingsGroup(label = "Polar") {
        ToggleRow(
            title = "Haptic feedback",
            subtitle = "Vibrate on long-press & edit",
            checked = state.hapticFeedback,
            onCheckedChange = onHapticChange,
            showDivider = false
        )
    }
    SettingsGroup(label = "Android") {
        NavRow(
            title = "Sound settings",
            subtitle = "Volume, ringtone, Do Not Disturb",
            onClick = { PolarSettingsIntents.open(context, Settings.ACTION_SOUND_SETTINGS) },
            showDivider = false
        )
    }
}

@Composable
internal fun DisplaySettingsPage(
    state: LauncherUiState,
    onReduceTransparencyChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    SettingsPageTitle("Display")
    SettingsGroup(label = "Polar") {
        ToggleRow(
            title = "Reduce transparency",
            subtitle = "Flattens Liquid Glass sheen & refraction",
            checked = state.reduceTransparency,
            onCheckedChange = onReduceTransparencyChange,
            showDivider = false
        )
    }
    SettingsGroup(label = "Android") {
        NavRow(
            title = "Brightness & display",
            subtitle = "Brightness, dark mode, timeout",
            onClick = { PolarSettingsIntents.open(context, Settings.ACTION_DISPLAY_SETTINGS) },
            showDivider = true
        )
        NavRow(
            title = "Wallpaper",
            subtitle = "Polar wallpaper picker",
            onClick = {
                PolarSettingsIntents.polarHome(context, extra = "open_wallpaper_picker")
                (context as? android.app.Activity)?.finish()
            },
            showDivider = false
        )
    }
}

@Composable
internal fun AccessibilitySettingsPage(
    state: LauncherUiState,
    onReduceMotionChange: (Boolean) -> Unit,
    onReduceTransparencyChange: (Boolean) -> Unit
) {
    SettingsPageTitle("Accessibility")
    SettingsGroup(label = "Motion") {
        ToggleRow(
            title = "Reduce motion",
            subtitle = "Snappier Polar animations, no app morph",
            checked = state.reduceMotion,
            onCheckedChange = onReduceMotionChange,
            showDivider = false
        )
    }
    SettingsGroup(label = "Display") {
        ToggleRow(
            title = "Reduce transparency",
            subtitle = "Less blur on glass",
            checked = state.reduceTransparency,
            onCheckedChange = onReduceTransparencyChange,
            showDivider = false
        )
    }
}

@Composable
internal fun PrivacySettingsPage(hasWallpaperAccess: Boolean, onGrantWallpaperAccess: () -> Unit) {
    val context = LocalContext.current
    rememberSettingsTick()
    SettingsPageTitle("Privacy")
    SettingsGroup(label = "Polar access") {
        NavRow(
            title = "Wallpaper & files",
            subtitle = if (hasWallpaperAccess) "Allowed" else "Needed for Liquid Glass",
            onClick = {
                if (hasWallpaperAccess) PolarSettingsIntents.appDetails(context)
                else onGrantWallpaperAccess()
            },
            showDivider = true
        )
        NavRow(
            title = "Notifications",
            subtitle = PolarSettingsIntents.notificationsLabel(context),
            onClick = { NotificationMirror.openAccessSettings(context) },
            showDivider = true
        )
        NavRow(
            title = "App permissions",
            subtitle = "Phone, SMS, location, storage",
            onClick = { PolarSettingsIntents.appDetails(context) },
            showDivider = false
        )
    }
    SettingsGroup(label = "Android") {
        NavRow(
            title = "Privacy dashboard",
            subtitle = "Permission usage",
            onClick = {
                PolarSettingsIntents.open(context, Settings.ACTION_PRIVACY_SETTINGS)
            },
            showDivider = true
        )
        NavRow(
            title = "Location",
            subtitle = "System location settings",
            onClick = { PolarSettingsIntents.open(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
            showDivider = false
        )
    }
}

@Composable
internal fun StatusBarSettingsPage(
    state: LauncherUiState,
    onShowBatteryPercentChange: (Boolean) -> Unit
) {
    SettingsPageTitle("Status Bar")
    SettingsGroup(label = "Polar") {
        ToggleRow(
            title = "Battery percentage",
            subtitle = "Digits inside the Polar battery",
            checked = state.showBatteryPercent,
            onCheckedChange = onShowBatteryPercentChange,
            showDivider = false
        )
    }
    Text(
        text = "Polar hides the Android status bar on Home. Time is on the left; Wi‑Fi, cellular, and battery are on the right. Pull left for Notification Center, right for Control Center.",
        style = MaterialTheme.typography.bodyMedium,
        color = VoidMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
internal fun SearchAssistantPage(
    state: LauncherUiState,
    onShowHomeSearchChange: (Boolean) -> Unit,
    onShowAssistantChange: (Boolean) -> Unit
) {
    SettingsPageTitle("Search & Assistant")
    SettingsGroup(label = "Home") {
        ToggleRow(
            title = "Search",
            subtitle = "Search pill above the dock",
            checked = state.showHomeSearch,
            onCheckedChange = onShowHomeSearchChange,
            showDivider = true
        )
        ToggleRow(
            title = "Polar Assistant",
            subtitle = "Home commands next to Search",
            checked = state.showAssistant,
            onCheckedChange = onShowAssistantChange,
            showDivider = false
        )
    }
}

@Composable
internal fun HiddenAppsPage(
    hiddenApps: List<AppInfo>,
    onUnhide: (String) -> Unit
) {
    SettingsPageTitle("Hidden Apps")
    if (hiddenApps.isEmpty()) {
        Text(
            text = "No hidden apps. Long-press an icon on Polar Home to hide it.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        return
    }
    SettingsGroup(label = "${hiddenApps.size} hidden") {
        hiddenApps.forEachIndexed { index, app ->
            NavRow(
                title = app.label,
                subtitle = "Tap to unhide",
                onClick = { onUnhide(app.key) },
                showDivider = index != hiddenApps.lastIndex
            )
        }
    }
}
