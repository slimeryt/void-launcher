package com.voidlauncher.app.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.BuildConfig
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.update.UpdateUiState
import kotlin.math.cos
import kotlin.math.sin

private val UpdatesCardBg = Color(0xFF1C1F26)
private val CancelGrey = Color(0xFF3A3A3C)
private val ButtonShape = RoundedCornerShape(28.dp)

@Composable
fun UpdatesScreen(
    updateState: UpdateUiState,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onCancelCheck: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = updateState.checking || updateState.downloading
    // Keep split visible briefly while busy; collapse when idle unless user just finished mid-split
    var forceSplit by remember { mutableStateOf(false) }
    val splitTarget = if (busy || forceSplit) 1f else 0f
    val split = remember { Animatable(0f) }

    LaunchedEffect(busy) {
        if (busy) {
            forceSplit = true
            split.animateTo(
                1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f)
            )
        } else if (forceSplit) {
            // Linger a beat then merge droplets back
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
            .background(VoidInk)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Header — Back + chevron
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = IosBlue,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Back",
                style = MaterialTheme.typography.titleLarge,
                color = IosBlue
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Big ColorOS-style update hero
            UpdateHeroArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Software Update",
                style = MaterialTheme.typography.headlineMedium,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            val status = when {
                updateState.error != null -> updateState.error
                updateState.checking -> "Looking for updates…"
                updateState.downloading -> "Downloading update…"
                updateState.downloadedApk != null -> "Ready to install"
                updateState.available != null ->
                    "Update ${updateState.available.versionName} is available"
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

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Version card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(UpdatesCardBg)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                VersionRow("Software version", "Void ${updateState.currentVersion}")
                Spacer(modifier = Modifier.height(14.dp))
                VersionRow("Version", updateState.currentVersion)
                Spacer(modifier = Modifier.height(14.dp))
                VersionRow("Build", BuildConfig.VERSION_CODE.toString())
                updateState.available?.let { rel ->
                    Spacer(modifier = Modifier.height(14.dp))
                    VersionRow("Latest", rel.versionName, accent = true)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // 4. Footer droplet button(s)
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
            onDownload = onDownloadUpdate,
            onInstall = onInstallUpdate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp, top = 8.dp)
        )
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
    val pulse = rememberInfiniteTransition(label = "hero")
    val glow by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val spin by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.38f

        // Soft backdrop glow (ColorOS-like)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    IosBlue.copy(alpha = 0.28f * glow),
                    IosBlue.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.85f
            ),
            radius = r * 1.85f,
            center = Offset(cx, cy)
        )

        // Main orb
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF5B9DFF),
                    IosBlue,
                    Color(0xFF0A5ECC)
                ),
                start = Offset(cx - r, cy - r),
                end = Offset(cx + r, cy + r)
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // Glass sheen
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                radius = r * 0.9f
            ),
            radius = r * 0.85f,
            center = Offset(cx - r * 0.15f, cy - r * 0.2f)
        )

        // Orbit ring
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = r * 1.22f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Orbiting dot
        val rad = Math.toRadians(spin.toDouble())
        val ox = cx + cos(rad).toFloat() * r * 1.22f
        val oy = cy + sin(rad).toFloat() * r * 1.22f
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 5.dp.toPx(), center = Offset(ox, oy))

        // Up arrow (update metaphor)
        val arrow = Path().apply {
            val top = cy - r * 0.28f
            val bot = cy + r * 0.32f
            val mid = cx
            val half = r * 0.22f
            moveTo(mid, top)
            lineTo(mid + half, top + half * 1.1f)
            moveTo(mid, top)
            lineTo(mid - half, top + half * 1.1f)
            moveTo(mid, top)
            lineTo(mid, bot)
        }
        drawPath(
            path = arrow,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
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
        // Left droplet — Cancel
        Box(
            modifier = Modifier
                .weight(cancelWeight)
                .fillMaxSize()
                .graphicsLayer {
                    // Squash/stretch like a separating drop
                    val squash = 1f + (1f - split) * 0.08f
                    scaleX = (0.55f + 0.45f * split) * squash
                    scaleY = 0.92f + 0.08f * split
                    alpha = split
                }
                .clip(ButtonShape)
                .background(CancelGrey)
                .clickable(enabled = split > 0.85f && busy) { onCancel() },
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

        // Right / single droplet — primary action
        Box(
            modifier = Modifier
                .weight(primaryWeight)
                .fillMaxSize()
                .graphicsLayer {
                    val stretch = if (split < 0.5f) 1f + (0.5f - split) * 0.06f else 1f
                    scaleX = stretch
                }
                .clip(ButtonShape)
                .background(IosBlue)
                .clickable(enabled = !busy) {
                    when {
                        updateState.downloadedApk != null -> onInstall()
                        updateState.available != null -> onDownload()
                        else -> onCheck()
                    }
                },
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
                    // Progress / search bar inside the blue droplet
                    val progress = when {
                        updateState.downloading -> updateState.progress.coerceIn(0.02f, 1f)
                        else -> -1f // indeterminate search
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

@Composable
private fun DropletProgressBar(progress: Float) {
    val indeterminate = rememberInfiniteTransition(label = "search-bar")
    val sweep by indeterminate.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
    ) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
        if (progress < 0f) {
            val barW = size.width * 0.35f
            val x = (size.width + barW) * sweep - barW
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(x, 0f),
                size = Size(barW, size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
        } else {
            drawRoundRect(
                color = Color.White,
                size = Size(size.width * progress, size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
        }
    }
}
