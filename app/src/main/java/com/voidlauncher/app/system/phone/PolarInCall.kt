package com.voidlauncher.app.system.phone

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.app.system.setPolarSystemContent
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted

object PolarInCall {
    @Volatile var service: PolarInCallService? = null
    @Volatile var call: Call? = null
}

class PolarInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        PolarInCall.service = this
        PolarInCall.call = call
        startActivity(
            Intent(this, PolarInCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    override fun onCallRemoved(call: Call) {
        if (PolarInCall.call == call) PolarInCall.call = null
    }
}

class PolarInCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPolarSystemContent { PolarInCallScreen(onDone = { finish() }) }
    }
}

@Composable
private fun PolarInCallScreen(onDone: () -> Unit) {
    val call = PolarInCall.call
    var state by remember { mutableIntStateOf(call?.state ?: Call.STATE_DISCONNECTED) }
    DisposableEffect(call) {
        if (call == null) {
            onDone()
            return@DisposableEffect onDispose { }
        }
        val cb = object : Call.Callback() {
            override fun onStateChanged(c: Call, newState: Int) {
                state = newState
                if (newState == Call.STATE_DISCONNECTED || newState == Call.STATE_DISCONNECTING) {
                    onDone()
                }
            }
        }
        call.registerCallback(cb)
        onDispose { runCatching { call.unregisterCallback(cb) } }
    }
    val handle = call?.details?.handle?.schemeSpecificPart ?: "Unknown"
    val context = LocalContext.current
    val contactName = remember(handle) { PolarContacts.lookupName(context, handle) }
    val ringing = state == Call.STATE_RINGING || state == Call.STATE_NEW
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = if (ringing) "Incoming call" else "Polar Phone",
                color = VoidMuted,
                fontSize = 15.sp
            )
            Text(
                contactName ?: handle,
                color = VoidMist,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (contactName != null) {
                Text(handle, color = VoidMuted, fontSize = 18.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (ringing) {
                RoundCallButton(Color(0xFF30D158), Icons.Rounded.Call, "Answer") {
                    PolarInCall.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            }
            RoundCallButton(Color(0xFFFF453A), Icons.Rounded.CallEnd, "End") {
                PolarInCall.call?.disconnect()
                onDone()
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun RoundCallButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CapsuleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(label, color = VoidMist, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
