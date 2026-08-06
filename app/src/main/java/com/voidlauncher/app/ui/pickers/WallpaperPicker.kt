package com.voidlauncher.app.ui.pickers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.components.GlassPanel
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.wallpaper.LocalWallpaperApi

private data class WallpaperSwatch(
    val id: String,
    val label: String,
    val colors: List<Color>
)

private val PresetWallpapers = listOf(
    WallpaperSwatch("ink", "Ink", listOf(Color(0xFF05060A), Color(0xFF12141C))),
    WallpaperSwatch("midnight", "Midnight", listOf(Color(0xFF0B1B3A), Color(0xFF1A1033))),
    WallpaperSwatch("ocean", "Ocean", listOf(Color(0xFF042F2E), Color(0xFF0E4F66))),
    WallpaperSwatch("ember", "Ember", listOf(Color(0xFF2A0E0A), Color(0xFF6B2A1F))),
    WallpaperSwatch("forest", "Forest", listOf(Color(0xFF0B1F14), Color(0xFF1B4332))),
    WallpaperSwatch("violet", "Violet", listOf(Color(0xFF1A0B2E), Color(0xFF4C1D95))),
    WallpaperSwatch("slate", "Slate", listOf(Color(0xFF111827), Color(0xFF334155))),
    WallpaperSwatch("rose", "Rose", listOf(Color(0xFF2A0A16), Color(0xFF9F1239))),
    WallpaperSwatch("sand", "Sand", listOf(Color(0xFF1C1410), Color(0xFF78716C))),
    WallpaperSwatch("aurora", "Aurora", listOf(Color(0xFF04111A), Color(0xFF115E59), Color(0xFF312E81))),
    WallpaperSwatch("peach", "Peach", listOf(Color(0xFF1A0F0C), Color(0xFF9A3412))),
    WallpaperSwatch("glacier", "Glacier", listOf(Color(0xFF0C1222), Color(0xFF334155), Color(0xFF94A3B8)))
)

@Composable
fun WallpaperPickerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val wallpaperApi = LocalWallpaperApi.current
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            wallpaperApi.onSetFromUri(uri)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 28.dp,
                strong = true,
                enableSheen = false,
                enableRefraction = true
            ) {
                WallpaperPickerBody(
                    onDismiss = onDismiss,
                    onPickPhoto = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSystem = {
                        wallpaperApi.onOpenSystemPicker()
                        onDismiss()
                    },
                    onSwatch = { colors ->
                        wallpaperApi.onSetGradient(colors)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperPickerBody(
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onSystem: () -> Unit,
    onSwatch: (List<Color>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wallpaper",
                style = MaterialTheme.typography.titleLarge,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = VoidMist)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WallpaperActionRow(
                icon = Icons.Rounded.Image,
                title = "Choose photo",
                subtitle = "From your gallery",
                onClick = onPickPhoto
            )
            WallpaperActionRow(
                icon = Icons.Rounded.Wallpaper,
                title = "System wallpapers",
                subtitle = "Live / OEM catalogs",
                onClick = onSystem
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Colors",
            color = VoidMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(PresetWallpapers, key = { it.id }) { swatch ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(SmoothCornerShape(18.dp))
                        .clickable { onSwatch(swatch.colors) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(SmoothCornerShape(18.dp))
                            .background(Brush.verticalGradient(swatch.colors))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = SmoothCornerShape(18.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = swatch.label,
                        color = VoidMist,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(42.dp)
                .clip(SmoothCornerShape(12.dp))
                .background(IosBlue.copy(alpha = 0.22f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IosBlue)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VoidMist,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = VoidMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
