package com.voidlauncher.app.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.account.AccountUiState
import com.voidlauncher.app.account.DeveloperAccountStatus
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.system.PolarRoles
import com.voidlauncher.app.system.PolarSystemApps
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.update.UpdateChannel
import com.voidlauncher.app.update.UpdateUiState
import com.voidlauncher.app.viewmodel.LauncherUiState

private enum class SettingsPage {
    Root, Account, LiquidGlass, HomeLayout, Updates, General, PolarApps, About,
    Notifications, Haptics, Display, Accessibility, Privacy, StatusBar, SearchAssistant, HiddenApps
}

@Composable
fun SettingsContent(
    state: LauncherUiState,
    updateState: UpdateUiState,
    accountState: AccountUiState,
    hasWallpaperAccess: Boolean,
    onGrantWallpaperAccess: () -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onDockLabelsChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onShowHomeSearchChange: (Boolean) -> Unit,
    onShowAssistantChange: (Boolean) -> Unit,
    onShowBatteryPercentChange: (Boolean) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onReduceTransparencyChange: (Boolean) -> Unit,
    onUnhideApp: (String) -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onGlassFrostChange: (Float) -> Unit,
    onGlassRefractionChange: (Boolean) -> Unit,
    onGlassSheenChange: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onCancelUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onUpdateChannelChange: (UpdateChannel) -> Unit,
    onMarkBetaChannelAgreed: (UpdateChannel) -> Unit,
    onAccountLogin: (String, String) -> Unit,
    onAccountRegister: (String, String, String) -> Unit,
    onAccountLogout: () -> Unit,
    onRequestDeveloperAccount: () -> Unit,
    onRequestEnroll: () -> Unit,
    onAccountRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenAppIcons: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(SettingsPage.Root) }
    var searchQuery by remember { mutableStateOf("") }
    val hazeState = LocalHazeState.current

    BackHandler {
        if (page != SettingsPage.Root) page = SettingsPage.Root
        else onBack()
    }

    if (page == SettingsPage.Updates) {
        UpdatesScreen(
            updateState = updateState,
            accountState = accountState,
            onBack = { page = SettingsPage.Root },
            onCheckUpdate = onCheckUpdate,
            onCancelCheck = onCancelUpdate,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate,
            onChannelChange = onUpdateChannelChange,
            onMarkChannelAgreed = onMarkBetaChannelAgreed,
            onOpenAccount = { page = SettingsPage.Account },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (page == SettingsPage.Account) {
        AccountScreen(
            accountState = accountState,
            onBack = { page = SettingsPage.Root },
            onLogin = onAccountLogin,
            onRegister = onAccountRegister,
            onLogout = onAccountLogout,
            onRequestDeveloperAccount = onRequestDeveloperAccount,
            onRequestEnroll = onRequestEnroll,
            onRefresh = onAccountRefresh,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (page == SettingsPage.About) {
        AboutScreen(
            onBack = { page = SettingsPage.Root },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(
            onBack = {
                if (page != SettingsPage.Root) {
                    page = SettingsPage.Root
                    searchQuery = ""
                } else {
                    onBack()
                }
            }
        )

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == SettingsPage.Root) {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                }
            },
            label = "settings-page",
            modifier = Modifier
                .weight(1f)
                .settingsHazeSource(hazeState)
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (current) {
                    SettingsPage.Root -> RootPage(
                        updateState = updateState,
                        accountState = accountState,
                        searchQuery = searchQuery,
                        hiddenCount = state.hiddenCount,
                        onOpen = {
                            searchQuery = ""
                            page = it
                        },
                        onOpenAppIcons = onOpenAppIcons
                    )
                    SettingsPage.LiquidGlass -> LiquidGlassPage(
                        state = state,
                        hasWallpaperAccess = hasWallpaperAccess,
                        onGrantWallpaperAccess = onGrantWallpaperAccess,
                        onGlassBlurChange = onGlassBlurChange,
                        onGlassFrostChange = onGlassFrostChange,
                        onGlassRefractionChange = onGlassRefractionChange,
                        onGlassSheenChange = onGlassSheenChange
                    )
                    SettingsPage.HomeLayout -> HomeLayoutPage(
                        state = state,
                        onShowLabelsChange = onShowLabelsChange,
                        onDockLabelsChange = onDockLabelsChange,
                        onGridColumnsChange = onGridColumnsChange,
                        onIconScaleChange = onIconScaleChange
                    )
                    SettingsPage.Updates -> Unit
                    SettingsPage.Account -> Unit
                    SettingsPage.About -> Unit
                    SettingsPage.PolarApps -> PolarAppsPage()
                    SettingsPage.Notifications -> NotificationsSettingsPage()
                    SettingsPage.Haptics -> SoundsHapticsPage(state, onHapticChange)
                    SettingsPage.Display -> DisplaySettingsPage(state, onReduceTransparencyChange)
                    SettingsPage.Accessibility -> AccessibilitySettingsPage(
                        state,
                        onReduceMotionChange,
                        onReduceTransparencyChange
                    )
                    SettingsPage.Privacy -> PrivacySettingsPage(
                        hasWallpaperAccess,
                        onGrantWallpaperAccess
                    )
                    SettingsPage.StatusBar -> StatusBarSettingsPage(state, onShowBatteryPercentChange)
                    SettingsPage.SearchAssistant -> SearchAssistantPage(
                        state,
                        onShowHomeSearchChange,
                        onShowAssistantChange
                    )
                    SettingsPage.HiddenApps -> HiddenAppsPage(state.hiddenApps, onUnhideApp)
                    SettingsPage.General -> GeneralPage(
                        state = state,
                        onHapticChange = onHapticChange
                    )
                }
            }
        }

        if (page == SettingsPage.Root) {
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }
    }
}

@Composable
private fun RootPage(
    updateState: UpdateUiState,
    accountState: AccountUiState,
    searchQuery: String,
    hiddenCount: Int,
    onOpen: (SettingsPage) -> Unit,
    onOpenAppIcons: () -> Unit
) {
    val q = searchQuery.trim().lowercase()
    fun matches(vararg parts: String): Boolean =
        q.isEmpty() || parts.any { it.lowercase().contains(q) }

    Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )

    val accountSubtitle = when {
        !accountState.signedIn -> "Sign in"
        accountState.developerEnrolled -> "${accountState.email} · Developer"
        accountState.isDeveloperAccount -> "${accountState.email} · Dev account"
        accountState.developerAccountStatus == DeveloperAccountStatus.Pending ->
            "${accountState.email} · Pending"
        else -> accountState.email.ifBlank { "Signed in" }
    }
    val context = LocalContext.current
    rememberSettingsTick()
    val showAccount = matches("Account", accountSubtitle, "sign in", "developer")
    val showWifi = matches("Wi-Fi", "wifi", "WLAN", "network")
    val showBluetooth = matches("Bluetooth", "network")
    val showCellular = matches("Cellular", "Mobile", "data", "network")
    val showNotifications = matches("Notifications", "Notification Center")
    val showWallpaper = matches("Wallpaper", "Appearance")
    val showLiquid = matches("Liquid Glass", "Blur", "frost", "refraction", "Appearance")
    val showIcons = matches("App Icons", "Icons", "theme", "tint", "Appearance")
    val showHome = matches("Home Screen", "labels", "grid", "Appearance")
    val showSearch = matches("Search", "Assistant", "Home")
    val showStatusBar = matches("Status Bar", "battery", "time")
    val showPolarApps = matches("Polar Apps", "Phone", "Messages", "dialer", "SMS")
    val showHidden = matches("Hidden Apps", "hide")
    val showHaptics = matches("Sounds", "Haptics", "vibrate")
    val showDisplay = matches("Display", "Brightness", "transparency")
    val showAccess = matches("Accessibility", "motion", "reduce")
    val showPrivacy = matches("Privacy", "permissions", "location")
    val showHomeRole = matches("Default Home", "launcher")
    val showDate = matches("Date", "Time")
    val showLanguage = matches("Language", "Region")
    val showBattery = matches("Battery")
    val updatesSubtitle = "v${updateState.currentVersion}" +
        (updateState.available?.let { " · ${it.versionName} available" } ?: "")
    val showUpdates = matches("Updates", updatesSubtitle)
    val showGeneral = matches("General", "Haptics", "behavior", PolarSettingsIntents.modelLabel())
    val showAbout = matches("About", "Version", "privacy", "licenses")

    if (showAccount) {
        SettingsGroup(label = "Account") {
            NavRow(
                title = "Account",
                subtitle = accountSubtitle,
                onClick = { onOpen(SettingsPage.Account) },
                showDivider = false
            )
        }
    }

    if (showWifi || showBluetooth || showCellular || showNotifications) {
        SettingsGroup(label = "Network") {
            if (showWifi) {
                NavRow(
                    title = "Wi-Fi",
                    subtitle = PolarSettingsIntents.wifiLabel(context),
                    onClick = { PolarSettingsIntents.open(context, android.provider.Settings.ACTION_WIFI_SETTINGS) },
                    showDivider = showBluetooth || showCellular || showNotifications
                )
            }
            if (showBluetooth) {
                NavRow(
                    title = "Bluetooth",
                    subtitle = PolarSettingsIntents.bluetoothLabel(context),
                    onClick = { PolarSettingsIntents.open(context, android.provider.Settings.ACTION_BLUETOOTH_SETTINGS) },
                    showDivider = showCellular || showNotifications
                )
            }
            if (showCellular) {
                NavRow(
                    title = "Cellular",
                    subtitle = "Mobile data & carrier",
                    onClick = {
                        PolarSettingsIntents.open(
                            context,
                            android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS
                        )
                    },
                    showDivider = showNotifications
                )
            }
            if (showNotifications) {
                NavRow(
                    title = "Notifications",
                    subtitle = PolarSettingsIntents.notificationsLabel(context),
                    onClick = { onOpen(SettingsPage.Notifications) },
                    showDivider = false
                )
            }
        }
    }

    if (showWallpaper || showLiquid || showIcons || showHome || showSearch || showStatusBar) {
        SettingsGroup(label = "Polar Home") {
            if (showWallpaper) {
                NavRow(
                    title = "Wallpaper",
                    subtitle = "Photos, colors, live wallpaper",
                    onClick = {
                        PolarSettingsIntents.polarHome(context, extra = "open_wallpaper_picker")
                        (context as? android.app.Activity)?.finish()
                    },
                    showDivider = showLiquid || showIcons || showHome || showSearch || showStatusBar
                )
            }
            if (showLiquid) {
                NavRow(
                    title = "Liquid Glass",
                    subtitle = "Blur, frost, refraction",
                    onClick = { onOpen(SettingsPage.LiquidGlass) },
                    showDivider = showIcons || showHome || showSearch || showStatusBar
                )
            }
            if (showIcons) {
                NavRow(
                    title = "App Icons",
                    subtitle = "Theme, radius, tint",
                    onClick = onOpenAppIcons,
                    showDivider = showHome || showSearch || showStatusBar
                )
            }
            if (showHome) {
                NavRow(
                    title = "Home Screen",
                    subtitle = "Labels & grid",
                    onClick = { onOpen(SettingsPage.HomeLayout) },
                    showDivider = showSearch || showStatusBar
                )
            }
            if (showSearch) {
                NavRow(
                    title = "Search & Assistant",
                    subtitle = "Home search pill",
                    onClick = { onOpen(SettingsPage.SearchAssistant) },
                    showDivider = showStatusBar
                )
            }
            if (showStatusBar) {
                NavRow(
                    title = "Status Bar",
                    subtitle = "Time, battery, pull gestures",
                    onClick = { onOpen(SettingsPage.StatusBar) },
                    showDivider = false
                )
            }
        }
    }

    if (showPolarApps || showHidden) {
        SettingsGroup(label = "Apps") {
            if (showPolarApps) {
                NavRow(
                    title = "Polar Apps",
                    subtitle = "Phone & Messages",
                    onClick = { onOpen(SettingsPage.PolarApps) },
                    showDivider = showHidden
                )
            }
            if (showHidden) {
                NavRow(
                    title = "Hidden Apps",
                    subtitle = if (hiddenCount == 0) "None hidden" else "$hiddenCount hidden",
                    onClick = { onOpen(SettingsPage.HiddenApps) },
                    showDivider = false
                )
            }
        }
    }

    if (showHaptics || showDisplay || showAccess || showPrivacy) {
        SettingsGroup(label = "Device") {
            if (showHaptics) {
                NavRow(
                    title = "Sounds & Haptics",
                    subtitle = "Vibration & volume",
                    onClick = { onOpen(SettingsPage.Haptics) },
                    showDivider = showDisplay || showAccess || showPrivacy
                )
            }
            if (showDisplay) {
                NavRow(
                    title = "Display",
                    subtitle = "Brightness & transparency",
                    onClick = { onOpen(SettingsPage.Display) },
                    showDivider = showAccess || showPrivacy
                )
            }
            if (showAccess) {
                NavRow(
                    title = "Accessibility",
                    subtitle = "Motion & transparency",
                    onClick = { onOpen(SettingsPage.Accessibility) },
                    showDivider = showPrivacy
                )
            }
            if (showPrivacy) {
                NavRow(
                    title = "Privacy",
                    subtitle = "Permissions Polar uses",
                    onClick = { onOpen(SettingsPage.Privacy) },
                    showDivider = false
                )
            }
        }
    }

    if (showHomeRole || showDate || showLanguage || showBattery || showUpdates || showGeneral || showAbout) {
        SettingsGroup(label = "System") {
            if (showHomeRole) {
                NavRow(
                    title = "Default Home",
                    subtitle = "Set Polar as the Home app",
                    onClick = { PolarSettingsIntents.open(context, android.provider.Settings.ACTION_HOME_SETTINGS) },
                    showDivider = showDate || showLanguage || showBattery || showUpdates || showGeneral || showAbout
                )
            }
            if (showDate) {
                NavRow(
                    title = "Date & Time",
                    subtitle = "24-hour time, time zone",
                    onClick = { PolarSettingsIntents.open(context, android.provider.Settings.ACTION_DATE_SETTINGS) },
                    showDivider = showLanguage || showBattery || showUpdates || showGeneral || showAbout
                )
            }
            if (showLanguage) {
                NavRow(
                    title = "Language & Region",
                    subtitle = "System locale",
                    onClick = { PolarSettingsIntents.open(context, android.provider.Settings.ACTION_LOCALE_SETTINGS) },
                    showDivider = showBattery || showUpdates || showGeneral || showAbout
                )
            }
            if (showBattery) {
                NavRow(
                    title = "Battery",
                    subtitle = "Charging & battery saver",
                    onClick = {
                        PolarSettingsIntents.open(
                            context,
                            android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
                        )
                    },
                    showDivider = showUpdates || showGeneral || showAbout
                )
            }
            if (showUpdates) {
                NavRow(
                    title = "Updates",
                    subtitle = updatesSubtitle,
                    onClick = { onOpen(SettingsPage.Updates) },
                    showDivider = showGeneral || showAbout
                )
            }
            if (showGeneral) {
                NavRow(
                    title = "General",
                    subtitle = PolarSettingsIntents.modelLabel(),
                    onClick = { onOpen(SettingsPage.General) },
                    showDivider = showAbout
                )
            }
            if (showAbout) {
                NavRow(
                    title = "About",
                    subtitle = "Version, privacy, licenses",
                    onClick = { onOpen(SettingsPage.About) },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassPage(
    state: LauncherUiState,
    hasWallpaperAccess: Boolean,
    onGrantWallpaperAccess: () -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onGlassFrostChange: (Float) -> Unit,
    onGlassRefractionChange: (Boolean) -> Unit,
    onGlassSheenChange: (Boolean) -> Unit
) {
    Text(
        text = "Liquid Glass",
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )

    if (!hasWallpaperAccess) {
        WallpaperAccessBanner(onGrantWallpaperAccess)
    }

    // Live preview — samples wallpaper (portal) so refraction/blur knobs are visible
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        cornerRadius = 28.dp,
        strong = true,
        enableSheen = state.glassSheen,
        enableRefraction = state.glassRefraction,
        sampleWallpaper = true
    ) {
        Text(
            text = "Liquid Glass preview",
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    SettingsGroup(label = "Look") {
        ToggleRow(
            title = "Refraction",
            subtitle = "Lens bend + chromatic edge",
            checked = state.glassRefraction,
            onCheckedChange = onGlassRefractionChange,
            showDivider = true
        )
        ToggleRow(
            title = "Sheen",
            subtitle = "Wet specular highlight",
            checked = state.glassSheen,
            onCheckedChange = onGlassSheenChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Intensity") {
        SliderBlock(
            title = "Blur strength",
            valueLabel = "${(state.glassBlurStrength * 100).toInt()}%",
            value = state.glassBlurStrength,
            onValueChange = onGlassBlurChange,
            // Down to 0 so "minimum" is actually clear glass, not a mandatory blur floor.
            valueRange = 0f..1.6f,
            showDivider = true
        )
        SliderBlock(
            title = "Frost amount",
            valueLabel = "${(state.glassFrostAmount * 100).toInt()}%",
            value = state.glassFrostAmount,
            onValueChange = onGlassFrostChange,
            valueRange = 0f..1.5f,
            showDivider = false
        )
    }

    Text(
        text = "Preview above updates live. Toggle refraction to see edge lens bend + " +
            "chromatic fringe; blur/frost/sheen drive the same AGSL on home dock / pills.",
        style = MaterialTheme.typography.bodyMedium,
        color = VoidMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun WallpaperAccessBanner(onGrantAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(SettingsCardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Wallpaper access needed",
            style = MaterialTheme.typography.titleMedium,
            color = VoidMist
        )
        Text(
            text = "Polar needs permission to read your wallpaper so Liquid Glass can blur " +
                "and refract it live. Without it, glass falls back to a flat look and these " +
                "settings won't visibly change anything.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidMuted
        )
        Text(
            text = "Grant access",
            style = MaterialTheme.typography.titleMedium,
            color = IosBlue,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onGrantAccess
                )
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeLayoutPage(
    state: LauncherUiState,
    onShowLabelsChange: (Boolean) -> Unit,
    onDockLabelsChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onIconScaleChange: (Float) -> Unit
) {
    Text(
        text = "Home Screen",
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )

    SettingsGroup(label = "Labels") {
        ToggleRow(
            title = "App labels",
            subtitle = "Show names under icons",
            checked = state.showLabels,
            onCheckedChange = onShowLabelsChange,
            showDivider = true
        )
        ToggleRow(
            title = "Dock labels",
            subtitle = "Names on dock icons",
            checked = state.dockLabels,
            onCheckedChange = onDockLabelsChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Size & grid") {
        SliderBlock(
            title = "Grid columns",
            valueLabel = "${state.gridColumns}",
            value = state.gridColumns.toFloat(),
            onValueChange = { onGridColumnsChange(it.toInt()) },
            valueRange = 3f..6f,
            steps = 2,
            showDivider = true
        )
        SliderBlock(
            title = "Icon scale",
            valueLabel = "${(state.iconScale * 100).toInt()}%",
            value = state.iconScale,
            onValueChange = onIconScaleChange,
            valueRange = 0.7f..1.3f,
            showDivider = false
        )
    }
}

@Composable
private fun PolarAppsPage() {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    Text(
        text = "Polar Apps",
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
    SettingsGroup(label = "Defaults") {
        NavRow(
            title = "Phone",
            subtitle = if (PolarRoles.isPhone(context)) "Default Phone" else "Open Polar Phone",
            onClick = {
                context.startActivity(
                    Intent().setClassName(context.packageName, PolarSystemApps.PhoneActivity)
                )
            },
            showDivider = true
        )
        NavRow(
            title = "Messages",
            subtitle = if (PolarRoles.isMessages(context)) "Default Messages" else "Open Polar Messages",
            onClick = {
                context.startActivity(
                    Intent().setClassName(context.packageName, PolarSystemApps.MessagesActivity)
                )
            },
            showDivider = false
        )
    }
    if (activity != null && (!PolarRoles.isPhone(context) || !PolarRoles.isMessages(context))) {
        SettingsGroup(label = "System roles") {
            if (!PolarRoles.isPhone(context)) {
                NavRow(
                    title = "Set as default Phone",
                    subtitle = "Calls stay in Polar",
                    onClick = {
                        PolarRoles.requestPhone(activity)?.let { activity.startActivity(it) }
                    },
                    showDivider = !PolarRoles.isMessages(context)
                )
            }
            if (!PolarRoles.isMessages(context)) {
                NavRow(
                    title = "Set as default Messages",
                    subtitle = "SMS stays in Polar",
                    onClick = {
                        PolarRoles.requestMessages(activity)?.let { activity.startActivity(it) }
                    },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun GeneralPage(
    state: LauncherUiState,
    onHapticChange: (Boolean) -> Unit
) {
    Text(
        text = "General",
        style = MaterialTheme.typography.headlineLarge,
        color = VoidMist,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )

    SettingsGroup(label = "Feedback") {
        ToggleRow(
            title = "Haptic feedback",
            subtitle = "Vibrate on long-press & edit",
            checked = state.hapticFeedback,
            onCheckedChange = onHapticChange,
            showDivider = false
        )
    }

    SettingsGroup(label = "Device") {
        InfoRow("Name", PolarSettingsIntents.modelLabel(), showDivider = true)
        InfoRow("Android", android.os.Build.VERSION.RELEASE.orEmpty(), showDivider = true)
        InfoRow("Apps", "${state.apps.size}", showDivider = true)
        InfoRow("Hidden", "${state.hiddenCount}", showDivider = true)
        InfoRow("Home pages", "${state.pages.size}", showDivider = false)
    }
}

@Composable
internal fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = VoidMuted,
            modifier = Modifier.padding(start = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SettingsCardShape)
                .background(SettingsCardBg)
        ) {
            content()
        }
    }
}

@Composable
internal fun NavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoidMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = VoidMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 18.dp)
            )
        }
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VoidMuted)
            }
            IosToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun SliderBlock(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    showDivider: Boolean,
    steps: Int = 0
) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
                Text(valueLabel, style = MaterialTheme.typography.titleMedium, color = IosBlue)
            }
            Spacer(modifier = Modifier.height(12.dp))
            IosSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
internal fun InfoRow(title: String, value: String, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VoidMist)
            Text(value, style = MaterialTheme.typography.titleMedium, color = VoidMuted)
        }
        if (showDivider) {
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val bg = when {
        !enabled -> SettingsChipBg.copy(alpha = 0.5f)
        filled -> IosBlue
        else -> SettingsChipBg
    }
    val fg = when {
        !enabled -> VoidMuted
        filled -> Color.White
        else -> VoidMist
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(SmoothCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
