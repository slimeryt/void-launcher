package com.voidlauncher.app.system.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voidlauncher.app.system.PolarRoleButton
import com.voidlauncher.app.system.PolarRoles
import com.voidlauncher.app.system.PolarSystemScaffold
import com.voidlauncher.app.system.setPolarSystemContent
import com.voidlauncher.app.ui.components.CapsuleShape
import com.voidlauncher.app.ui.theme.VoidMist
import com.voidlauncher.app.ui.theme.VoidMuted
import kotlinx.coroutines.flow.MutableStateFlow

class PolarPhoneActivity : ComponentActivity() {
    private val incomingDigits = MutableStateFlow("")
    private var chainScreening = false
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (chainScreening) {
                chainScreening = false
                PolarRoles.requestCallerId(this)?.let { startActivity(it) }
            }
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingDigits.value = digitsFrom(intent)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
        )
        setPolarSystemContent {
            val seed by incomingDigits.collectAsStateWithLifecycle()
            PolarPhoneScreen(
                initialDigits = seed,
                onRequestDefault = {
                    val phone = PolarRoles.requestPhone(this)
                    val screening = PolarRoles.requestCallerId(this)
                    when {
                        phone != null -> {
                            chainScreening = true
                            roleLauncher.launch(phone)
                        }
                        screening != null -> roleLauncher.launch(screening)
                        else -> Toast.makeText(
                            this,
                            "Polar is already the Phone app",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingDigits.value = digitsFrom(intent)
    }

    private fun digitsFrom(intent: Intent?): String {
        val raw = intent?.data?.schemeSpecificPart.orEmpty()
        return raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    }
}

private data class PolarCallRow(val name: String, val number: String, val whenText: String)
private data class PolarContactRow(val name: String, val number: String)

@Composable
private fun PolarPhoneScreen(
    initialDigits: String,
    onRequestDefault: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(2) }
    var digits by remember { mutableStateOf(initialDigits) }
    LaunchedEffect(initialDigits) {
        if (initialDigits.isNotBlank()) {
            digits = initialDigits
            tab = 2
        }
    }
    var recents by remember { mutableStateOf(emptyList<PolarCallRow>()) }
    var contacts by remember { mutableStateOf(emptyList<PolarContactRow>()) }
    val isDefault = PolarRoles.isPhone(context)

    LaunchedEffect(tab) {
        recents = loadRecents(context)
        contacts = loadContacts(context)
    }

    PolarSystemScaffold(
        title = "Phone",
        subtitle = if (isDefault) "Polar is the default Phone app" else "Set Polar as Phone to handle calls"
    ) {
        if (!isDefault) {
            PolarRoleButton(
                held = false,
                heldLabel = "Default Phone",
                actionLabel = "Set as Default Phone",
                onClick = onRequestDefault,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> CallList(recents, emptyLabel = "No recent calls") { digits = it.number; tab = 2 }
                1 -> CallList(
                    contacts.map { PolarCallRow(it.name, it.number, it.number) },
                    emptyLabel = "No contacts"
                ) { digits = it.number; tab = 2 }
                else -> Keypad(
                    digits = digits,
                    onDigits = { digits = it },
                    onCall = {
                        if (digits.isBlank()) return@Keypad
                        placeCall(context, digits)
                    }
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhoneTab("Recents", Icons.Rounded.History, tab == 0) { tab = 0 }
            PhoneTab("Contacts", Icons.Rounded.Contacts, tab == 1) { tab = 1 }
            PhoneTab("Keypad", Icons.Rounded.Dialpad, tab == 2) { tab = 2 }
        }
    }
}

@Composable
private fun PhoneTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Color(0xFF30D158) else VoidMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CallList(
    rows: List<PolarCallRow>,
    emptyLabel: String,
    onPick: (PolarCallRow) -> Unit
) {
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyLabel, color = VoidMuted)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(rows, key = { it.number + it.whenText + it.name }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(CapsuleShape)
                    .clickable { onPick(row) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.name, color = VoidMist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(row.whenText, color = VoidMuted, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.Call, contentDescription = "Call", tint = Color(0xFF30D158))
            }
        }
    }
}

@Composable
private fun Keypad(
    digits: String,
    onDigits: (String) -> Unit,
    onCall: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = digits.ifBlank { " " },
            color = VoidMist,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            maxLines = 1
        )
        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )
        keys.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (key, letters) ->
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(CapsuleShape)
                            .background(Color(0xFF2C2C2E))
                            .clickable { onDigits(digits + key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(key, color = VoidMist, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                            if (letters.isNotEmpty()) {
                                Text(letters, color = VoidMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(64.dp))
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CapsuleShape)
                    .background(Color(0xFF30D158))
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Box(
                Modifier
                    .size(64.dp)
                    .clickable(enabled = digits.isNotEmpty()) {
                        onDigits(digits.dropLast(1))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (digits.isNotEmpty()) {
                    Icon(Icons.Rounded.Backspace, contentDescription = "Delete", tint = VoidMist)
                }
            }
        }
    }
}

private fun loadRecents(context: android.content.Context): List<PolarCallRow> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
        PackageManager.PERMISSION_GRANTED
    ) return emptyList()
    val rows = mutableListOf<PolarCallRow>()
    runCatching {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            val nameI = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numI = c.getColumnIndex(CallLog.Calls.NUMBER)
            val dateI = c.getColumnIndex(CallLog.Calls.DATE)
            while (c.moveToNext() && rows.size < 80) {
                val number = c.getString(numI).orEmpty()
                if (number.isBlank()) continue
                val cached = c.getString(nameI)?.ifBlank { null }
                val name = cached ?: PolarContacts.lookupName(context, number) ?: number
                rows += PolarCallRow(name, number, android.text.format.DateFormat.format("MMM d, h:mm a", c.getLong(dateI)).toString())
            }
        }
    }
    return rows
}

private fun loadContacts(context: android.content.Context): List<PolarContactRow> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
        PackageManager.PERMISSION_GRANTED
    ) return emptyList()
    val rows = mutableListOf<PolarContactRow>()
    runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { c ->
            val nameI = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numI = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext() && rows.size < 200) {
                val number = c.getString(numI).orEmpty()
                val name = c.getString(nameI).orEmpty().ifBlank { number }
                if (number.isNotBlank()) rows += PolarContactRow(name, number)
            }
        }
    }
    return rows.distinctBy { it.number }
}

private fun placeCall(context: android.content.Context, digits: String) {
    val uri = Uri.parse("tel:${Uri.encode(digits)}")
    if (PolarRoles.isPhone(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val placed = telecom != null && runCatching {
            telecom.placeCall(uri, null)
            true
        }.getOrDefault(false)
        if (placed) return
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        runCatching { context.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    } else {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

