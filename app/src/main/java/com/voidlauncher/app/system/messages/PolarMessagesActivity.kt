package com.voidlauncher.app.system.messages

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.app.system.PolarRoleButton
import com.voidlauncher.app.system.PolarRoles
import com.voidlauncher.app.system.PolarSystemScaffold
import com.voidlauncher.app.system.setPolarSystemContent
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.flow.collectLatest

class PolarMessagesActivity : ComponentActivity() {
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_MMS
            )
        )
        val seedAddress = intent.data?.schemeSpecificPart
            ?: intent.getStringExtra("address")
        val seedBody = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra("sms_body")
        setPolarSystemContent {
            PolarMessagesScreen(
                seedAddress = seedAddress.orEmpty(),
                seedBody = seedBody.orEmpty(),
                onRequestDefault = {
                    val intent = PolarRoles.requestMessages(this)
                    if (intent != null) roleLauncher.launch(intent)
                    else Toast.makeText(this, "Polar is already Messages", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun PolarMessagesScreen(
    seedAddress: String,
    seedBody: String,
    onRequestDefault: () -> Unit
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(seedAddress.takeIf { it.isNotBlank() }) }
    var threads by remember { mutableStateOf(PolarSms.threads(context)) }
    val isDefault = PolarRoles.isMessages(context)

    LaunchedEffect(Unit) {
        PolarSms.changes.collectLatest { threads = PolarSms.threads(context) }
    }
    LaunchedEffect(isDefault) {
        threads = PolarSms.threads(context)
    }

    val thread = open
    if (thread != null) {
        PolarConversation(
            address = thread,
            seedBody = seedBody,
            onBack = { open = null }
        )
        return
    }

    PolarSystemScaffold(
        title = "Messages",
        subtitle = if (isDefault) "Polar is the default Messages app" else "Set Polar as Messages to send and receive SMS"
    ) {
        if (!isDefault) {
            PolarRoleButton(
                held = false,
                heldLabel = "Default Messages",
                actionLabel = "Set as Default Messages",
                onClick = onRequestDefault,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        PolarComposeNew(onOpen = { open = it })
        Spacer(Modifier.height(12.dp))
        if (threads.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No messages yet", color = VoidMuted)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(threads, key = { it.address }) { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(CapsuleShape)
                            .clickable { open = row.address }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(row.address, color = VoidMist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            row.snippet,
                            color = VoidMuted,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolarComposeNew(onOpen: (String) -> Unit) {
    var number by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PolarField(
            value = number,
            onValueChange = { number = it },
            hint = "New conversation number",
            modifier = Modifier.weight(1f)
        )
        Text(
            "Open",
            color = IosBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(
                    enabled = number.isNotBlank(),
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onOpen(number.trim()) }
                .padding(8.dp)
        )
    }
}

@Composable
private fun PolarConversation(address: String, seedBody: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(PolarSms.conversation(context, address)) }
    var draft by remember { mutableStateOf(seedBody) }
    val listState = rememberLazyListState()

    LaunchedEffect(address) {
        PolarSms.changes.collectLatest {
            messages = PolarSms.conversation(context, address)
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = IosBlue,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Text(
                address,
                color = VoidMist,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val bubble = if (msg.outgoing) Color(0xFF30D158) else Color(0xFF2C2C2E)
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.outgoing) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        msg.body,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(bubble)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PolarField(
                value = draft,
                onValueChange = { draft = it },
                hint = "iMessage / SMS",
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CapsuleShape)
                    .background(if (draft.isBlank()) Color(0xFF2C2C2E) else IosBlue)
                    .clickable(enabled = draft.isNotBlank()) {
                        if (PolarSms.send(context, address, draft.trim())) {
                            draft = ""
                            messages = PolarSms.conversation(context, address)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PolarField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(CapsuleShape)
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(hint, color = VoidMuted, fontSize = 16.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            cursorBrush = SolidColor(IosBlue),
            textStyle = TextStyle(color = VoidMist, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
