package com.voidlauncher.app.ui.pickers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import com.voidlauncher.app.ui.statusbar.polarStatusPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.Image
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.HomeFolder
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.components.SmoothCornerShape
import com.voidlauncher.app.ui.components.toCachedBitmap
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageManagerOverlay(
    visible: Boolean,
    pages: List<List<HomeItem>>,
    appsByKey: Map<String, AppInfo>,
    folders: Map<String, HomeFolder>,
    gridColumns: Int,
    onDismiss: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val pageCount = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val appearance = LocalIconAppearance.current

    // Keep pager in range when a page is deleted; jump to new page when one is added
    var prevCount by remember { mutableIntStateOf(pageCount) }
    LaunchedEffect(pageCount) {
        if (pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage((pageCount - 1).coerceAtLeast(0))
        } else if (pageCount > prevCount) {
            pagerState.animateScrollToPage(pageCount - 1)
        }
        prevCount = pageCount
    }

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
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
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pages",
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
                    val items = pages.getOrElse(page) { emptyList() }
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
                                .background(Color(0xFF1A1C24))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.14f),
                                    SmoothCornerShape(28.dp)
                                )
                                .padding(10.dp)
                        ) {
                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Empty",
                                        color = VoidMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns.coerceIn(3, 6)),
                                    userScrollEnabled = false,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = items,
                                        key = { item ->
                                            when (item) {
                                                is HomeItem.App -> "a:${item.key}"
                                                is HomeItem.Folder -> "f:${item.id}"
                                            }
                                        }
                                    ) { item ->
                                        when (item) {
                                            is HomeItem.App -> {
                                                val app = appsByKey[item.key]
                                                if (app != null) {
                                                    PagePreviewApp(
                                                        app = app,
                                                        radiusRatio = appearance.cornerRadiusRatio
                                                    )
                                                }
                                            }
                                            is HomeItem.Folder -> {
                                                PagePreviewFolder(
                                                    name = folders[item.id]?.name ?: "Folder",
                                                    apps = folders[item.id]?.appKeys
                                                        ?.mapNotNull { appsByKey[it] }
                                                        .orEmpty(),
                                                    radiusRatio = appearance.cornerRadiusRatio
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Page ${page + 1}",
                            color = VoidMist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (val n = items.size) {
                                0 -> "No apps"
                                1 -> "1 item"
                                else -> "$n items"
                            },
                            color = VoidMuted,
                            style = MaterialTheme.typography.labelMedium
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
                repeat(pageCount) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (selected) VoidMist else VoidMist.copy(alpha = 0.28f))
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(i) }
                            }
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
                        .height(48.dp)
                        .clip(CapsuleShape)
                        .background(Color(0xFF2C2C2E))
                        .clickable(
                            enabled = pageCount > 1,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onRemovePage(pagerState.currentPage) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = if (pageCount > 1) Color(0xFFFF6B6B) else VoidMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delete",
                            color = if (pageCount > 1) Color(0xFFFF6B6B) else VoidMuted,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CapsuleShape)
                        .background(IosBlue)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAddPage
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Add page",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PagePreviewApp(
    app: AppInfo,
    radiusRatio: Float,
    modifier: Modifier = Modifier
) {
    val bmp = remember(app.key, radiusRatio) {
        app.icon.toCachedBitmap(96, cornerRadiusRatio = radiusRatio).asImageBitmap()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low,
            modifier = Modifier
                .size(48.dp)
                .clip(SmoothCornerShape(percent = (radiusRatio * 100f).toInt().coerceIn(1, 50)))
        )
        Text(
            text = app.label,
            color = VoidMist,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PagePreviewFolder(
    name: String,
    apps: List<AppInfo>,
    radiusRatio: Float,
    modifier: Modifier = Modifier
) {
    val shape = SmoothCornerShape(percent = (radiusRatio * 100f).toInt().coerceIn(1, 50))
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(Color(0x66FFFFFF))
                .padding(5.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0 until 2) {
                            val app = apps.getOrNull(row * 2 + col)
                            if (app != null) {
                                val bmp = remember(app.key, radiusRatio) {
                                    app.icon.toCachedBitmap(48, cornerRadiusRatio = radiusRatio)
                                        .asImageBitmap()
                                }
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    filterQuality = FilterQuality.Low,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .clip(shape)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = name,
            color = VoidMist,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
