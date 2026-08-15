package com.voidlauncher.app.ui.pickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import com.voidlauncher.app.ui.statusbar.polarStatusPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.wallpaper.LocalWallpaperApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

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

private fun decodeWallpaperBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxEdge = 4096
        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        while (longest / sample > maxEdge) sample *= 2
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decode)
        }
    }.getOrNull()
}

/**
 * Cover-fit the source into [viewportW]x[viewportH], apply user scale/offset, then
 * rasterize into [outW]x[outH] (typically 2× screen width for parallax).
 */
private fun renderEditedWallpaper(
    source: Bitmap,
    viewportW: Float,
    viewportH: Float,
    userScale: Float,
    offset: Offset,
    outW: Int,
    outH: Int
): Bitmap {
    val cover = max(viewportW / source.width, viewportH / source.height)
    val total = cover * userScale.coerceAtLeast(1f)
    val drawW = source.width * total
    val drawH = source.height * total
    val left = (viewportW - drawW) / 2f + offset.x
    val top = (viewportH - drawH) / 2f + offset.y

    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    canvas.drawColor(android.graphics.Color.BLACK)
    val sx = outW / viewportW
    val sy = outH / viewportH
    canvas.scale(sx, sy)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, null, RectF(left, top, left + drawW, top + drawH), paint)
    return out
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperPickerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val wallpaperApi = LocalWallpaperApi.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PresetWallpapers.size })
    var editBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var applying by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { decodeWallpaperBitmap(context, uri) }
            if (bmp != null) editBitmap = bmp
        }
    }

    BackHandler {
        when {
            editBitmap != null -> editBitmap = null
            else -> onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidInk)
            .polarStatusPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val editing = editBitmap
        if (editing != null) {
            WallpaperQuickEdit(
                source = editing,
                applying = applying,
                onCancel = { editBitmap = null },
                onApply = { scale, offset, viewportW, viewportH ->
                    if (applying) return@WallpaperQuickEdit
                    applying = true
                    scope.launch {
                        val out = withContext(Dispatchers.Default) {
                            val dm = context.resources.displayMetrics
                            renderEditedWallpaper(
                                source = editing,
                                viewportW = viewportW,
                                viewportH = viewportH,
                                userScale = scale,
                                offset = offset,
                                outW = dm.widthPixels.coerceAtLeast(1080) * 2,
                                outH = dm.heightPixels.coerceAtLeast(1920)
                            )
                        }
                        wallpaperApi.onSetBitmap(out)
                        applying = false
                        editBitmap = null
                        onDismiss()
                    }
                }
            )
        } else {
            WallpaperPresetBrowser(
                pagerState = pagerState,
                onDismiss = onDismiss,
                onApplyPreset = {
                    wallpaperApi.onSetGradient(PresetWallpapers[pagerState.currentPage].colors)
                    onDismiss()
                },
                onChoosePhoto = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onDotClick = { i -> scope.launch { pagerState.animateScrollToPage(i) } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperPresetBrowser(
    pagerState: PagerState,
    onDismiss: () -> Unit,
    onApplyPreset: () -> Unit,
    onChoosePhoto: () -> Unit,
    onDotClick: (Int) -> Unit
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 36.dp),
                pageSpacing = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val swatch = PresetWallpapers[page]
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        val s = lerp(0.88f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = s
                        scaleY = s
                        alpha = lerp(0.55f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.58f)
                            .clip(SmoothCornerShape(28.dp))
                            .background(Brush.verticalGradient(swatch.colors))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.14f),
                                SmoothCornerShape(28.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = swatch.label,
                        color = VoidMist,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(PresetWallpapers.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (selected) VoidMist else VoidMist.copy(alpha = 0.28f))
                        .clickable { onDotClick(i) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CapsuleShape)
                    .background(IosBlue)
                    .clickable(onClick = onApplyPreset),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Apply",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onChoosePhoto),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Image, "Choose photo", tint = VoidMist, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun WallpaperQuickEdit(
    source: Bitmap,
    applying: Boolean,
    onCancel: () -> Unit,
    onApply: (scale: Float, offset: Offset, viewportW: Float, viewportH: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val imageBitmap = remember(source) { source.asImageBitmap() }
    var viewportW by remember { mutableFloatStateOf(1f) }
    var viewportH by remember { mutableFloatStateOf(1f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                style = MaterialTheme.typography.titleLarge,
                color = VoidMist,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            IconButton(onClick = onCancel, enabled = !applying) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = VoidMist)
            }
        }

        Text(
            text = "Pinch to zoom · drag to move",
            color = VoidMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxH = maxHeight * 0.92f
            val idealH = maxWidth / 0.58f
            val frameH = min(idealH.value, maxH.value).dp
            val frameW = frameH * 0.58f
            viewportW = with(density) { frameW.toPx() }
            viewportH = with(density) { frameH.toPx() }

            Box(
                modifier = Modifier
                    .size(frameW, frameH)
                    .clip(SmoothCornerShape(28.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), SmoothCornerShape(28.dp))
                    .pointerInput(source.width, source.height) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 4f)
                            val cover = max(
                                viewportW / source.width,
                                viewportH / source.height
                            )
                            val drawW = source.width * cover * nextScale
                            val drawH = source.height * cover * nextScale
                            val maxX = ((drawW - viewportW) / 2f).coerceAtLeast(0f)
                            val maxY = ((drawH - viewportH) / 2f).coerceAtLeast(0f)
                            scale = nextScale
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(enabled = !applying, onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = VoidMist, style = MaterialTheme.typography.titleMedium)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CapsuleShape)
                    .background(if (applying) IosBlue.copy(alpha = 0.5f) else IosBlue)
                    .clickable(enabled = !applying) {
                        onApply(scale, offset, viewportW, viewportH)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (applying) "Applying…" else "Set Wallpaper",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
