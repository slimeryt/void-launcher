package com.voidlauncher.app.system

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidInk
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import com.voidlauncher.app.ui.theme.VoidTheme

fun ComponentActivity.setPolarSystemContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.BLACK
    window.navigationBarColor = android.graphics.Color.BLACK
    setContent {
        VoidTheme {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(VoidInk)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PolarSystemScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            color = VoidMist,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = VoidMuted,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        content()
    }
}

@Composable
fun PolarRoleButton(
    held: Boolean,
    heldLabel: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (held) Color(0xFF2C2C2E) else IosBlue
    val fg = if (held) VoidMist else Color.White
    Box(
        modifier
            .fillMaxWidth()
            .clip(CapsuleShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = !held
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (held) heldLabel else actionLabel,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}
