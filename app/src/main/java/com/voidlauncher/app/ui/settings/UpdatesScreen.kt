package com.voidlauncher.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.BuildConfig
import com.voidlauncher.app.R
import com.voidlauncher.app.account.AccountUiState
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.Android16ProgressBar
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.glass.LocalHazeState
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.update.UpdateChannel
import com.voidlauncher.app.update.UpdateUiState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val UpdatesCardBg = SettingsCardBg
private val CardShape = SettingsCardShape

@Composable
fun UpdatesScreen(
    updateState: UpdateUiState,
    accountState: AccountUiState,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onCancelCheck: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onChannelChange: (UpdateChannel) -> Unit,
    onMarkChannelAgreed: (UpdateChannel) -> Unit = {},
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBetaPicker by remember { mutableStateOf(false) }
    var pendingChannel by remember { mutableStateOf<UpdateChannel?>(null) }
    var pendingDownloadAgree by remember { mutableStateOf(false) }

    BackHandler {
        when {
            pendingChannel != null -> pendingChannel = null
            pendingDownloadAgree -> pendingDownloadAgree = false
            showBetaPicker -> showBetaPicker = false
            else -> onBack()
        }
    }

    pendingChannel?.let { channel ->
        BetaSoftwareAgreementDialog(
            channel = channel,
            onAgree = {
                onChannelChange(channel)
                pendingChannel = null
                showBetaPicker = false
            },
            onCancel = { pendingChannel = null }
        )
    }

    if (pendingDownloadAgree) {
        val kind = updateState.available?.channelKind ?: "beta"
        val channel = when (kind) {
            "developer" -> UpdateChannel.Developer
            else -> UpdateChannel.PublicBeta
        }
        BetaSoftwareAgreementDialog(
            channel = channel,
            titleOverride = "Install ${channel.label} Update?",
            onAgree = {
                pendingDownloadAgree = false
                onMarkChannelAgreed(channel)
                onDownloadUpdate()
            },
            onCancel = { pendingDownloadAgree = false }
        )
    }

    if (showBetaPicker) {
        BetaUpdatesPickerScreen(
            selected = updateState.channel,
            accountState = accountState,
            onSelect = { channel ->
                if (channel == updateState.channel) {
                    showBetaPicker = false
                    return@BetaUpdatesPickerScreen
                }
                if (channel == UpdateChannel.Off) {
                    onChannelChange(channel)
                    showBetaPicker = false
                    return@BetaUpdatesPickerScreen
                }
                val alreadyAgreed = when (channel) {
                    UpdateChannel.PublicBeta -> updateState.agreedPublicBeta
                    UpdateChannel.Developer -> updateState.agreedDeveloperBeta
                    UpdateChannel.Off -> true
                }
                if (alreadyAgreed) {
                    onChannelChange(channel)
                    showBetaPicker = false
                } else {
                    pendingChannel = channel
                }
            },
            onBack = { showBetaPicker = false },
            modifier = modifier
        )
        return
    }

    val busy = updateState.checking || updateState.downloading
    var forceSplit by remember { mutableStateOf(false) }
    val split = remember { Animatable(0f) }
    val hazeState = LocalHazeState.current

    LaunchedEffect(busy) {
        if (busy) {
            forceSplit = true
            split.animateTo(
                1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f)
            )
        } else if (forceSplit) {
            kotlinx.coroutines.delay(180)
            forceSplit = false
            split.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .settingsHazeSource(hazeState)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            UpdateHeroArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Software Update",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            val channelHint = when (updateState.channel) {
                UpdateChannel.Off -> null
                UpdateChannel.PublicBeta -> "Public Beta"
                UpdateChannel.Developer -> "Developer"
            }
            val status = when {
                updateState.error != null -> updateState.error
                updateState.checking -> "Looking for updates…"
                updateState.downloading -> "Downloading update…"
                updateState.downloadedApk != null -> "Ready to install"
                updateState.available != null -> {
                    val kind = when (updateState.available.channelKind) {
                        "beta" -> "Public Beta "
                        "developer" -> "Developer "
                        else -> ""
                    }
                    "${kind}Update ${updateState.available.versionName} is available"
                }
                updateState.statusMessage.isNotBlank() -> updateState.statusMessage
                else -> "Your software is up to date"
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = if (updateState.error != null) Color(0xFFF87171) else VoidMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(UpdatesCardBg)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                VersionRow("Software version", "Polar ${updateState.currentVersion}")
                Spacer(modifier = Modifier.height(14.dp))
                VersionRow("Version", updateState.currentVersion)
                Spacer(modifier = Modifier.height(14.dp))
                VersionRow("Build", BuildConfig.VERSION_CODE.toString())
                if (channelHint != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    VersionRow("Channel", channelHint, accent = true)
                }
                updateState.available?.let { rel ->
                    Spacer(modifier = Modifier.height(14.dp))
                    VersionRow("Latest", rel.versionName, accent = true)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Apple-style Beta Updates enrollment
            Text(
                text = "Beta Updates",
                style = MaterialTheme.typography.titleSmall,
                color = VoidMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(UpdatesCardBg)
                    .clickable { showBetaPicker = true }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Beta Updates",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VoidMist,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = updateState.channel.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VoidMuted
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = VoidMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        UpdateDropletActions(
            split = split.value,
            updateState = updateState,
            busy = busy,
            onCheck = {
                forceSplit = true
                onCheckUpdate()
            },
            onCancel = {
                onCancelCheck()
                forceSplit = false
            },
            onDownload = {
                val kind = updateState.available?.channelKind
                val needsAgree = when (kind) {
                    "developer" -> !updateState.agreedDeveloperBeta
                    "beta" -> !updateState.agreedPublicBeta
                    else -> false
                }
                if (needsAgree) {
                    pendingDownloadAgree = true
                } else {
                    onDownloadUpdate()
                }
            },
            onInstall = onInstallUpdate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp, top = 8.dp)
        )
    }
}

@Composable
private fun BetaUpdatesPickerScreen(
    selected: UpdateChannel,
    accountState: AccountUiState,
    onSelect: (UpdateChannel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showDeveloper = accountState.developerEnrolled
    val hazeState = LocalHazeState.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsBackBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .settingsHazeSource(hazeState)
        ) {
            Text(
                text = "Beta Updates",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(CardShape)
                    .background(UpdatesCardBg)
            ) {
            ChannelOption(
                title = "Off",
                subtitle = "Only public releases",
                selected = selected == UpdateChannel.Off,
                onClick = { onSelect(UpdateChannel.Off) },
                showDivider = true
            )
            ChannelOption(
                title = "Public Beta",
                subtitle = "Near-final builds before public release",
                selected = selected == UpdateChannel.PublicBeta,
                onClick = { onSelect(UpdateChannel.PublicBeta) },
                showDivider = showDeveloper
            )
            if (showDeveloper) {
                ChannelOption(
                    title = "Developer",
                    subtitle = "Earliest builds — may be unstable",
                    selected = selected == UpdateChannel.Developer,
                    onClick = { onSelect(UpdateChannel.Developer) },
                    showDivider = false
                )
            }
            }
        }
    }
}

@Composable
private fun BetaSoftwareAgreementDialog(
    channel: UpdateChannel,
    onAgree: () -> Unit,
    onCancel: () -> Unit,
    titleOverride: String? = null
) {
    val title = titleOverride ?: "Agree to ${channel.label} Terms?"
    val body = when (channel) {
        UpdateChannel.Developer ->
            "Developer builds are early, unfinished software. They may be unstable, incomplete, " +
                "or remove features without notice.\n\n" +
                "By agreeing, you understand Polar Developer software is for testing only, " +
                "is not a finished product, and Polar is not responsible for data loss or device issues.\n\n" +
                "You can leave Developer anytime by setting Beta Updates to Off or Public Beta."
        UpdateChannel.PublicBeta ->
            "Public Beta includes features that may change, break, or be removed before a public release.\n\n" +
                "By agreeing, you understand Polar Public Beta is pre-release software provided for " +
                "testing, and Polar is not responsible for data loss or unexpected behavior.\n\n" +
                "You can leave Public Beta anytime by setting Beta Updates to Off."
        UpdateChannel.Off -> ""
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(SmoothCornerShape(24.dp))
                .background(UpdatesCardBg)
                .padding(22.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = VoidMuted
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CapsuleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = VoidMist, style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CapsuleShape)
                        .background(IosBlue)
                        .clickable(onClick = onAgree),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Agree",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VoidMist,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VoidMuted
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

@Composable
private fun VersionRow(label: String, value: String, accent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = VoidMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (accent) IosBlue else VoidMist,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UpdateHeroArt(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.update_hero),
        contentDescription = "Software update",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun UpdateDropletActions(
    split: Float,
    updateState: UpdateUiState,
    busy: Boolean,
    onCheck: () -> Unit,
    onCancel: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = (12f * split).dp
    val cancelWeight = (0.0001f + 0.92f * split).coerceAtLeast(0.0001f)
    val primaryWeight = (2f - split).coerceAtLeast(0.08f)

    val primaryLabel = when {
        updateState.downloading -> "Downloading"
        updateState.checking -> "Searching"
        updateState.downloadedApk != null -> "Install"
        updateState.available != null -> "Download"
        else -> "Check for updates"
    }

    Row(
        modifier = modifier.height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(cancelWeight)
                .fillMaxSize()
                .graphicsLayer {
                    val squash = 1f + (1f - split) * 0.08f
                    scaleX = (0.55f + 0.45f * split) * squash
                    scaleY = 0.92f + 0.08f * split
                    alpha = split
                }
                .clickable(enabled = split > 0.85f && busy) { onCancel() },
            cornerRadius = 28.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (split > 0.35f) {
                    Text(
                        text = "Cancel",
                        color = VoidMist,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }

        GlassPanel(
            modifier = Modifier
                .weight(primaryWeight)
                .fillMaxSize()
                .graphicsLayer {
                    val stretch = if (split < 0.5f) 1f + (0.5f - split) * 0.06f else 1f
                    scaleX = stretch
                }
                .clickable(enabled = !busy) {
                    when {
                        updateState.downloadedApk != null -> onInstall()
                        updateState.available != null -> onDownload()
                        else -> onCheck()
                    }
                },
            cornerRadius = 28.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true,
            tint = IosBlue.copy(alpha = 0.55f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (busy) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = primaryLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        val progress = when {
                            updateState.downloading -> updateState.progress.coerceIn(0.02f, 1f)
                            else -> -1f
                        }
                        DropletProgressBar(progress = progress)
                    }
                } else {
                    Text(
                        text = primaryLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DropletProgressBar(progress: Float) {
    Android16ProgressBar(
        progress = progress,
        modifier = Modifier.fillMaxWidth(),
        activeColor = Color.White,
        trackColor = Color.White.copy(alpha = 0.28f)
    )
}
