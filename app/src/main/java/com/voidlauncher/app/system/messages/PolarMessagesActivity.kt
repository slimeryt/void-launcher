package com.voidlauncher.app.system.messages

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voidlauncher.app.system.PolarRoleButton
import com.voidlauncher.app.system.PolarRoles
import com.voidlauncher.app.system.setPolarSystemContent
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.IosBlue
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class PolarMessagesRoute {
    Inbox, Settings
}

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
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf(PolarMessagesRoute.Inbox) }
    var open by remember { mutableStateOf(seedAddress.takeIf { it.isNotBlank() }) }
    var threads by remember { mutableStateOf(PolarSms.threads(context)) }
    val isDefault = PolarRoles.isMessages(context)
    val wifiSending by PolarMessagesSettings.wifiSendingFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)

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
            isDefault = isDefault,
            wifiSending = wifiSending,
            onRequestDefault = onRequestDefault,
            onOpenSettings = { route = PolarMessagesRoute.Settings },
            onBack = { open = null }
        )
        return
    }

    if (route == PolarMessagesRoute.Settings) {
        PolarMessagesSettingsScreen(
            wifiSending = wifiSending,
            onWifiSendingChange = { enabled ->
                scope.launch { PolarMessagesSettings.setWifiSending(context, enabled) }
            },
            onBack = { route = PolarMessagesRoute.Inbox }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Messages",
                    color = VoidMist,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDefault) {
                        "Polar is the default Messages app"
                    } else {
                        "Set Polar as Messages to send and receive SMS"
                    },
                    color = VoidMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Messages settings",
                tint = IosBlue,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { route = PolarMessagesRoute.Settings }
                    )
            )
        }
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
private fun PolarMessagesSettingsScreen(
    wifiSending: Boolean,
    onWifiSendingChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
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
                "Settings",
                color = VoidMist,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Text(
            text = "SENDING",
            color = VoidMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CapsuleShape)
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Wi-Fi sending", color = VoidMist, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Try SMS/MMS over Wi-Fi Calling",
                        color = VoidMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = wifiSending,
                    onCheckedChange = onWifiSendingChange
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "ABOUT WI-FI SENDING",
            color = VoidMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Text(
            text = PolarMessagesSettings.WIFI_SENDING_DISCLAIMER,
            color = VoidMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
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
private fun PolarConversation(
    address: String,
    seedBody: String,
    isDefault: Boolean,
    wifiSending: Boolean,
    onRequestDefault: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(PolarSms.conversation(context, address)) }
    var draft by remember { mutableStateOf(seedBody) }
    var pendingAttachment by remember { mutableStateOf<Uri?>(null) }
    var pendingMime by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingAttachment = uri
        pendingMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    LaunchedEffect(address) {
        PolarSms.changes.collectLatest {
            messages = PolarSms.conversation(context, address)
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    fun sendCurrent() {
        if (!isDefault) {
            Toast.makeText(context, "Set Polar as default Messages to send", Toast.LENGTH_SHORT).show()
            onRequestDefault()
            return
        }
        val body = draft.trim()
        val attachment = pendingAttachment
        val mime = pendingMime
        val isMms = attachment != null && mime != null
        when (PolarMessagesNetwork.sendBlockReason(context, wifiSending, isMms)) {
            PolarSendNetworkBlock.WifiOnlyDisabled -> {
                Toast.makeText(
                    context,
                    "Enable Wi-Fi sending in Messages settings, or turn on mobile data",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            PolarSendNetworkBlock.NoConnection -> {
                Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show()
                return
            }
            null -> Unit
        }
        val ok = when {
            isMms -> PolarMms.send(context, address, attachment!!, mime!!, body, wifiSending)
            body.isNotBlank() -> PolarSms.send(context, address, body, wifiSending)
            else -> false
        }
        if (ok) {
            draft = ""
            pendingAttachment = null
            pendingMime = null
            messages = PolarSms.conversation(context, address)
        } else {
            Toast.makeText(context, "Could not send message", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
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
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            )
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Messages settings",
                tint = IosBlue,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSettings
                    )
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { "${it.id}-${it.date}" }) { msg ->
                MessageBubble(msg)
            }
        }
        pendingAttachment?.let { uri ->
            PendingAttachmentRow(
                uri = uri,
                mime = pendingMime,
                onClear = {
                    pendingAttachment = null
                    pendingMime = null
                }
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CapsuleShape)
                    .background(Color(0xFF2C2C2E))
                    .clickable { attachLauncher.launch(arrayOf("image/*", "video/*", "application/*")) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Attach", tint = VoidMist)
            }
            PolarField(
                value = draft,
                onValueChange = { draft = it },
                hint = "Text message",
                modifier = Modifier.weight(1f)
            )
            val canSend = draft.isNotBlank() || pendingAttachment != null
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CapsuleShape)
                    .background(if (!canSend) Color(0xFF2C2C2E) else IosBlue)
                    .clickable(enabled = canSend) { sendCurrent() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: PolarSmsMessage) {
    val context = LocalContext.current
    val bubble = if (msg.outgoing) Color(0xFF30D158) else Color(0xFF2C2C2E)
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (msg.outgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(bubble)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            msg.attachmentUri?.let { uri ->
                if (msg.attachmentMime?.startsWith("image/") == true) {
                    val bitmap = remember(uri) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        }.getOrNull()
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                } else {
                    Text(
                        msg.attachmentMime ?: "Attachment",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
            if (msg.body.isNotBlank() && msg.body != "Attachment") {
                Text(msg.body, color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PendingAttachmentRow(uri: Uri, mime: String?, onClear: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(CapsuleShape)
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mime?.startsWith("image/") == true) {
            val bitmap = remember(uri) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
        }
        Text(
            text = mime ?: "Attachment",
            color = VoidMist,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Remove attachment",
            tint = VoidMuted,
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClear
                )
        )
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
