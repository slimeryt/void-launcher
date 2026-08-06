package com.voidlauncher.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.BuildConfig
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted

private val CardShape = SettingsCardShape

private enum class AboutDoc { None, Privacy, Licenses, Acknowledgments }

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doc by remember { mutableStateOf(AboutDoc.None) }

    if (doc != AboutDoc.None) {
        AboutDocumentScreen(
            title = when (doc) {
                AboutDoc.Privacy -> "Privacy Policy"
                AboutDoc.Licenses -> "Licenses"
                AboutDoc.Acknowledgments -> "Acknowledgments"
                AboutDoc.None -> ""
            },
            body = when (doc) {
                AboutDoc.Privacy -> PrivacyPolicyText
                AboutDoc.Licenses -> LicensesText
                AboutDoc.Acknowledgments -> AcknowledgmentsText
                AboutDoc.None -> ""
            },
            onBack = { doc = AboutDoc.None },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(SettingsCardBg)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                AboutMetaRow("Polar", "Launcher")
                Spacer(modifier = Modifier.height(14.dp))
                AboutMetaRow("Version", BuildConfig.VERSION_NAME)
                Spacer(modifier = Modifier.height(14.dp))
                AboutMetaRow("Build", BuildConfig.VERSION_CODE.toString())
            }

            SettingsGroup(label = "Legal") {
                AboutLinkRow(
                    title = "Privacy Policy",
                    onClick = { doc = AboutDoc.Privacy },
                    showDivider = true
                )
                AboutLinkRow(
                    title = "Licenses",
                    onClick = { doc = AboutDoc.Licenses },
                    showDivider = true
                )
                AboutLinkRow(
                    title = "Acknowledgments",
                    onClick = { doc = AboutDoc.Acknowledgments },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun AboutDocumentScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = VoidMuted
            )
        }
    }
}

@Composable
private fun AboutMetaRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = VoidMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = VoidMist,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    subtitle: String? = null
) {
    // Reuse SettingsScreen NavRow pattern inline to avoid private access.
    Column {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = VoidMist)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VoidMuted)
                }
            }
            Text("›", color = VoidMuted, style = MaterialTheme.typography.titleLarge)
        }
        if (showDivider) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

// SettingsGroup is private in SettingsScreen — duplicate a tiny local one.
@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = VoidMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(SettingsCardBg)
        ) {
            content()
        }
    }
}

private val PrivacyPolicyText = """
Last updated: August 6, 2026

Polar (“the app”) is a home-screen launcher for Android.

Information we process
• Account data you provide (email, display name, password) is sent to our accounts service so you can sign in, request a Developer Account, and enroll in Developer Beta.
• Update checks contact Polar’s update service to see if a newer build is available. This may expose a standard network request (IP address, user agent) to that service.
• Wallpaper, widgets, and launcher layout stay on your device unless you choose otherwise.

What we don’t do
• We do not sell your personal information.
• We do not require an account for basic launcher use (home screen, drawer, glass settings).

Developer features
• Applying for a Developer Account and enrolling in Developer Beta stores enrollment status with your account on our server.

Your choices
• You can sign out and clear the local session anytime in Settings → Account.
• You can leave Public Beta or Developer channels in Settings → Updates → Beta Updates.
""".trimIndent()

private val LicensesText = """
Polar includes third-party libraries under their respective licenses:

AndroidX Core / Activity / Lifecycle
License: Apache License 2.0

Jetpack Compose (UI, Foundation, Material3, Animation)
License: Apache License 2.0

Jetpack DataStore Preferences
License: Apache License 2.0

Accompanist (DrawablePainter, SystemUIController)
License: Apache License 2.0

Kotlin standard library & coroutines
License: Apache License 2.0

Full Apache License 2.0 text:
https://www.apache.org/licenses/LICENSE-2.0
""".trimIndent()

private val AcknowledgmentsText = """
Thanks for using Polar.

Built with Jetpack Compose and inspired by the clarity of modern system settings UIs.

Wallpaper blur, liquid glass, and software updates are maintained as part of Polar.
""".trimIndent()
