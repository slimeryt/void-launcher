package com.voidlauncher.app.ui.assistant

import com.voidlauncher.app.data.AppInfo

sealed class AssistantAction {
    data class Reply(val message: String) : AssistantAction()
    data class LaunchApp(val app: AppInfo, val message: String) : AssistantAction()
    data class OpenDrawer(val message: String = "Opening the app drawer.") : AssistantAction()
    data class SearchApps(val query: String, val message: String) : AssistantAction()
    data class OpenSettings(val message: String = "Opening Settings.") : AssistantAction()
    data class OpenSystemSettings(val action: String, val message: String) : AssistantAction()
    data class EditHome(val message: String = "Entering edit mode.") : AssistantAction()
    data object Dismiss : AssistantAction()
}

private val unknown = AssistantAction.Reply(
    "I'm not sure I understand, could you try again?"
)

/**
 * Rule-based assistant — not an LLM. Parses short home-screen commands.
 */
fun parseAssistantCommand(raw: String, apps: List<AppInfo>): AssistantAction {
    val text = raw.trim().replace(Regex("\\s+"), " ")
    if (text.isEmpty()) return unknown

    val lower = text.lowercase()

    // Dismiss
    if (lower in dismissPhrases || dismissPhrases.any { lower == it || lower.startsWith("$it ") }) {
        return AssistantAction.Dismiss
    }

    // Help
    if (lower in helpPhrases || lower.startsWith("help") || lower.contains("what can you do")) {
        return AssistantAction.Reply(
            "Try: Open [app], Search [name], Open settings, Show apps, Edit home, Wi‑Fi settings."
        )
    }

    // Edit home
    if (matchesAny(lower, "edit home", "edit mode", "rearrange", "organize home", "jiggle")) {
        return AssistantAction.EditHome()
    }

    // Settings (Polar)
    if (matchesAny(lower, "open settings", "open polar settings", "launcher settings", "settings") &&
        !lower.contains("wifi") && !lower.contains("wi-fi") && !lower.contains("bluetooth") &&
        !lower.contains("display") && !lower.contains("sound") && !lower.contains("battery")
    ) {
        return AssistantAction.OpenSettings()
    }

    // System settings panels
    when {
        matchesAny(lower, "wifi settings", "wi-fi settings", "open wifi", "open wi-fi", "wifi") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_WIFI_SETTINGS,
                "Opening Wi‑Fi settings."
            )
        matchesAny(lower, "bluetooth settings", "open bluetooth", "bluetooth") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                "Opening Bluetooth settings."
            )
        matchesAny(lower, "display settings", "brightness settings") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_DISPLAY_SETTINGS,
                "Opening display settings."
            )
        matchesAny(lower, "sound settings", "volume settings") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_SOUND_SETTINGS,
                "Opening sound settings."
            )
        matchesAny(lower, "battery settings", "battery") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS,
                "Opening battery settings."
            )
        matchesAny(lower, "app settings", "application settings") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_APPLICATION_SETTINGS,
                "Opening app settings."
            )
        matchesAny(lower, "date settings", "time settings", "date and time") ->
            return AssistantAction.OpenSystemSettings(
                android.provider.Settings.ACTION_DATE_SETTINGS,
                "Opening date & time settings."
            )
    }

    // Drawer
    if (matchesAny(
            lower,
            "open drawer",
            "show apps",
            "show all apps",
            "app drawer",
            "all apps",
            "app list",
            "open apps"
        )
    ) {
        return AssistantAction.OpenDrawer()
    }

    // Search apps
    searchPrefix(lower, text)?.let { query ->
        return if (query.isBlank()) {
            AssistantAction.SearchApps("", "Opening search.")
        } else {
            AssistantAction.SearchApps(query, "Searching for “$query”.")
        }
    }

    // Open / launch / start / run app
    openAppTarget(lower, text)?.let { name ->
        val app = findApp(apps, name)
        return if (app != null) {
            AssistantAction.LaunchApp(app, "Opening ${app.label}.")
        } else {
            AssistantAction.Reply("I couldn't find an app named “$name”.")
        }
    }

    // Bare app name (single token / short phrase matching an installed app)
    findApp(apps, text)?.let { app ->
        if (text.length >= 2) {
            return AssistantAction.LaunchApp(app, "Opening ${app.label}.")
        }
    }

    return unknown
}

private val dismissPhrases = setOf(
    "close", "dismiss", "cancel", "nevermind", "never mind", "go away",
    "stop", "exit", "bye", "goodbye"
)

private val helpPhrases = setOf(
    "help", "commands", "what can you do", "how do i", "how to"
)

private fun matchesAny(lower: String, vararg phrases: String): Boolean =
    phrases.any { p -> lower == p || lower.startsWith("$p ") || lower.endsWith(" $p") || lower.contains(p) }

private fun searchPrefix(lower: String, original: String): String? {
    val prefixes = listOf("search for ", "search ", "find ", "look for ", "lookup ")
    for (p in prefixes) {
        if (lower.startsWith(p)) {
            return original.substring(p.length).trim()
        }
    }
    if (lower == "search" || lower == "find") return ""
    return null
}

private fun openAppTarget(lower: String, original: String): String? {
    val prefixes = listOf(
        "open the ", "open ", "launch the ", "launch ", "start the ", "start ",
        "run the ", "run ", "go to ", "play "
    )
    for (p in prefixes) {
        if (lower.startsWith(p)) {
            var rest = original.substring(p.length).trim()
            // strip trailing "app" / "please"
            rest = rest.replace(Regex("""(?i)\s+(app|please|now)\s*$"""), "").trim()
            if (rest.isNotEmpty() &&
                !rest.equals("settings", ignoreCase = true) &&
                !rest.equals("drawer", ignoreCase = true) &&
                !rest.equals("apps", ignoreCase = true)
            ) {
                return rest
            }
        }
    }
    return null
}

internal fun findApp(apps: List<AppInfo>, query: String): AppInfo? {
    val q = query.trim()
    if (q.isEmpty()) return null
    val ql = q.lowercase()

    apps.firstOrNull { it.label.equals(q, ignoreCase = true) }?.let { return it }

    val starts = apps.filter { it.label.lowercase().startsWith(ql) }
    if (starts.size == 1) return starts.first()
    if (starts.isNotEmpty()) {
        return starts.minByOrNull { it.label.length }
    }

    val contains = apps.filter { it.label.lowercase().contains(ql) }
    if (contains.size == 1) return contains.first()
    if (contains.isNotEmpty()) {
        // Prefer shortest label that contains the query (e.g. "Chrome" over "Chrome Beta")
        return contains.minByOrNull { it.label.length }
    }

    // Token match: "youtube music" → label contains all tokens
    val tokens = ql.split(Regex("\\s+")).filter { it.length >= 2 }
    if (tokens.size >= 2) {
        val multi = apps.filter { app ->
            val l = app.label.lowercase()
            tokens.all { l.contains(it) }
        }
        if (multi.isNotEmpty()) return multi.minByOrNull { it.label.length }
    }

    return null
}
