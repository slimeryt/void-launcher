package com.voidlauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.ui.icons.LocalIconAppearance
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.util.PendingLaunchBounds
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    showLabel: Boolean,
    iconScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    selected: Boolean = false,
    /** When false, only tap is handled here — parent owns long-press / drag. */
    longPressEnabled: Boolean = true
) {
    val appearance = LocalIconAppearance.current
    val effectiveScale = appearance.scale
    val iconSize = (58 * effectiveScale).dp
    val radiusRatio = appearance.cornerRadiusRatio
    val shape = remember(appearance.shapePercent) {
        SmoothCornerShape(percent = appearance.shapePercent.coerceAtLeast(1))
    }
    val imageBitmap = remember(app.key, radiusRatio) {
        app.icon.toCachedBitmap(maxSize = 192, cornerRadiusRatio = radiusRatio).asImageBitmap()
    }
    val colorFilter = remember(appearance.theme, appearance.tintHue, appearance.tintAlpha) {
        appearance.colorFilter()
    }

    val boundsHolder = remember { mutableStateOf<android.graphics.Rect?>(null) }

    val clickModifier = when {
        editMode -> Modifier
        !longPressEnabled -> Modifier
        else -> Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                PendingLaunchBounds.rect = boundsHolder.value
                onClick()
            },
            onLongClick = {
                PendingLaunchBounds.rect = boundsHolder.value
                onLongClick()
            }
        )
    }

    Column(
        modifier = modifier
            .width(80.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                boundsHolder.value = android.graphics.Rect(
                    pos.x.roundToInt(),
                    pos.y.roundToInt(),
                    (pos.x + coords.size.width).roundToInt(),
                    (pos.y + coords.size.height).roundToInt()
                )
            }
            .then(clickModifier)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            Image(
                bitmap = imageBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                colorFilter = colorFilter,
                modifier = Modifier
                    .size(iconSize)
                    // Stroke outside clip so the rim follows the squircle edge (no gap / crop).
                    .liquidGlassStroke(shape = shape, strong = true)
                    .clip(shape)
            )
            if (editMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) IosBlue else Color(0xE63A3A3C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
