package com.voidlauncher.app.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted

@Composable
fun IconEditorPanel(
    draft: IconAppearance,
    onDraftChange: (IconAppearance) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // App Icons editor: liquid glass with fixed 50% backdrop blur (not home blur).
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            strong = true,
            enableSheen = true,
            enableRefraction = true,
            sampleWallpaper = true,
            blurStrengthOverride = 0.5f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: reset · title · size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeaderCircleButton(
                        onClick = { onDraftChange(IconAppearance.Default) },
                        contentDescription = "Reset to default"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = VoidMist,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "App Icons",
                        style = MaterialTheme.typography.titleMedium,
                        color = VoidMist
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderCircleButton(
                            onClick = {
                                onDraftChange(
                                    draft.copy(scale = (draft.scale - 0.05f).coerceIn(0.7f, 1.3f))
                                )
                            },
                            contentDescription = "Smaller icons"
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = null,
                                tint = VoidMist,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HeaderCircleButton(
                            onClick = {
                                onDraftChange(
                                    draft.copy(scale = (draft.scale + 0.05f).coerceIn(0.7f, 1.3f))
                                )
                            },
                            contentDescription = "Larger icons"
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = VoidMist,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Themes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconTheme.entries.forEach { theme ->
                        val selected = draft.theme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SmoothCornerShape(14.dp))
                                .background(
                                    if (selected) IosBlue.copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.10f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onDraftChange(draft.copy(theme = theme)) }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else VoidMist
                            )
                        }
                    }
                }

                IconSliderRow(
                    title = "Corner Radius",
                    valueLabel = "${draft.cornerRadiusPercent.toInt()}%",
                    value = draft.cornerRadiusPercent,
                    onValueChange = { onDraftChange(draft.copy(cornerRadiusPercent = it)) },
                    valueRange = 0f..50f
                )

                if (draft.theme == IconTheme.Tinted) {
                    HueSliderRow(
                        hue = draft.tintHue,
                        onHueChange = { onDraftChange(draft.copy(tintHue = it)) }
                    )
                    IconSliderRow(
                        title = "Alpha",
                        valueLabel = "${(draft.tintAlpha * 100f).toInt()}%",
                        value = draft.tintAlpha,
                        onValueChange = { onDraftChange(draft.copy(tintAlpha = it)) },
                        valueRange = 0.1f..1f
                    )
                }

                // Apply
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CapsuleShape)
                        .background(IosBlue)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onApply
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "Done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoidMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun IconSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = VoidMist)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = IosBlue)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = IosBlue,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun HueSliderRow(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Color", style = MaterialTheme.typography.bodyMedium, color = VoidMist)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hue.mod(360f), 0.72f, 1f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CapsuleShape)
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.hsv(0f, 0.85f, 1f),
                            Color.hsv(60f, 0.85f, 1f),
                            Color.hsv(120f, 0.85f, 1f),
                            Color.hsv(180f, 0.85f, 1f),
                            Color.hsv(240f, 0.85f, 1f),
                            Color.hsv(300f, 0.85f, 1f),
                            Color.hsv(360f, 0.85f, 1f)
                        )
                    )
                )
            }
            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
