package com.voidlauncher.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    val time = remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    }
    val date = remember(now) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now)
    }

    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            color = VoidMist
        )
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            color = VoidMuted
        )
    }
}
