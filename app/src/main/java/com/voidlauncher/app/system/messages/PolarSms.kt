package com.voidlauncher.app.system.messages

import android.Manifest
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.voidlauncher.app.system.PolarRoles
import kotlinx.coroutines.flow.MutableSharedFlow

data class PolarSmsThread(
    val address: String,
    val snippet: String,
    val date: Long
)

data class PolarSmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val outgoing: Boolean,
    val attachmentUri: Uri? = null,
    val attachmentMime: String? = null
)

object PolarSms {
    const val EXTRA_ROW_ID = "polar_sms_row_id"
    const val EXTRA_IS_MMS = "polar_sms_is_mms"
    private const val ACTION_SMS_SENT = "com.voidlauncher.app.SMS_SENT"
    private const val ACTION_MMS_SENT = "com.voidlauncher.app.MMS_SENT"

    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emitChange() {
        changes.tryEmit(Unit)
    }

    fun canRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun canSend(context: Context): Boolean =
        PolarRoles.isMessages(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    enum class SendFailure {
        NotDefaultApp,
        MissingSendPermission,
        NetworkBlocked
    }

    fun sendFailure(context: Context, wifiSending: Boolean, isMms: Boolean): SendFailure? {
        if (!PolarRoles.isMessages(context)) return SendFailure.NotDefaultApp
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SendFailure.MissingSendPermission
        }
        when (PolarMessagesNetwork.sendBlockReason(context, wifiSending, isMms)) {
            PolarSendNetworkBlock.WifiOnlyDisabled,
            PolarSendNetworkBlock.NoConnection -> return SendFailure.NetworkBlocked
            null -> Unit
        }
        return null
    }

    fun threads(context: Context): List<PolarSmsThread> {
        if (!canRead(context)) return emptyList()
        val grouped = linkedMapOf<String, PolarSmsThread>()
        loadSmsThreads(context, grouped)
        loadMmsThreads(context, grouped)
        return grouped.values.sortedByDescending { it.date }
    }

    fun conversation(context: Context, address: String): List<PolarSmsMessage> {
        if (!canRead(context)) return emptyList()
        val rows = mutableListOf<PolarSmsMessage>()
        loadSmsConversation(context, address, rows)
        loadMmsConversation(context, address, rows)
        return rows.sortedBy { it.date }
    }

    fun send(context: Context, address: String, body: String, wifiSending: Boolean = false): Boolean {
        if (address.isBlank() || body.isBlank()) return false
        if (sendFailure(context, wifiSending, isMms = false) != null) return false
        val sms = smsManager(context)
        return runCatching {
            val rowId = insertOutbox(context, address, body) ?: return false
            val sentIntent = pendingSentIntent(context, rowId, isMms = false)
            val parts = sms.divideMessage(body)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                    repeat(parts.size) { add(sentIntent) }
                }
                sms.sendMultipartTextMessage(address, null, parts, sentIntents, null)
            } else {
                sms.sendTextMessage(address, null, body, sentIntent, null)
            }
            emitChange()
            true
        }.getOrDefault(false)
    }

    fun pendingSentIntent(context: Context, rowId: Long, isMms: Boolean): PendingIntent {
        val action = if (isMms) PolarSmsSentReceiver.ACTION_MMS_SENT else PolarSmsSentReceiver.ACTION_SMS_SENT
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_ROW_ID, rowId)
            putExtra(EXTRA_IS_MMS, isMms)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            (rowId xor if (isMms) 0x100000 else 0).toInt(),
            intent,
            flags
        )
    }

    fun updateMessageBox(context: Context, rowId: Long, isMms: Boolean, boxOrType: Int) {
        if (isMms) {
            val values = ContentValues().apply {
                put(Telephony.Mms.MESSAGE_BOX, boxOrType)
            }
            runCatching {
                context.contentResolver.update(
                    ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, rowId),
                    values,
                    null,
                    null
                )
            }
        } else {
            val values = ContentValues().apply {
                put(Telephony.Sms.TYPE, boxOrType)
            }
            runCatching {
                context.contentResolver.update(
                    ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, rowId),
                    values,
                    null,
                    null
                )
            }
        }
    }

    fun insertInbox(context: Context, address: String, body: String) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        runCatching { context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values) }
        emitChange()
    }

    @Suppress("DEPRECATION")
    fun smsManager(context: Context): SmsManager {
        val subId = if (Build.VERSION.SDK_INT >= 22) {
            SubscriptionManager.getDefaultSmsSubscriptionId()
        } else {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        return when {
            subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID ->
                SmsManager.getSmsManagerForSubscriptionId(subId)
            Build.VERSION.SDK_INT >= 31 ->
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            else -> SmsManager.getDefault()
        }
    }

    private fun insertOutbox(context: Context, address: String, body: String): Long? {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
        }
        val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) ?: return null
        return ContentUris.parseId(uri)
    }

    private fun loadSmsThreads(context: Context, grouped: LinkedHashMap<String, PolarSmsThread>) {
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                val a = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val b = c.getColumnIndex(Telephony.Sms.BODY)
                val d = c.getColumnIndex(Telephony.Sms.DATE)
                while (c.moveToNext()) {
                    val address = c.getString(a).orEmpty()
                    if (address.isBlank()) continue
                    val existing = grouped[address]
                    val date = c.getLong(d)
                    if (existing == null || date > existing.date) {
                        grouped[address] = PolarSmsThread(
                            address = address,
                            snippet = c.getString(b).orEmpty(),
                            date = date
                        )
                    }
                }
            }
        }
    }

    private fun loadMmsThreads(context: Context, grouped: LinkedHashMap<String, PolarSmsThread>) {
        runCatching {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE),
                null,
                null,
                "${Telephony.Mms.DATE} DESC"
            )?.use { c ->
                val idI = c.getColumnIndex(Telephony.Mms._ID)
                val d = c.getColumnIndex(Telephony.Mms.DATE)
                while (c.moveToNext()) {
                    val mmsId = c.getLong(idI)
                    val address = mmsAddress(context, mmsId) ?: continue
                    val date = c.getLong(d) * 1000L
                    val snippet = mmsSnippet(context, mmsId)
                    val existing = grouped[address]
                    if (existing == null || date > existing.date) {
                        grouped[address] = PolarSmsThread(address, snippet, date)
                    }
                }
            }
        }
    }

    private fun loadSmsConversation(context: Context, address: String, rows: MutableList<PolarSmsMessage>) {
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
                ),
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(address),
                "${Telephony.Sms.DATE} ASC"
            )?.use { c ->
                val idI = c.getColumnIndex(Telephony.Sms._ID)
                val a = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val b = c.getColumnIndex(Telephony.Sms.BODY)
                val d = c.getColumnIndex(Telephony.Sms.DATE)
                val t = c.getColumnIndex(Telephony.Sms.TYPE)
                while (c.moveToNext()) {
                    val type = c.getInt(t)
                    rows += PolarSmsMessage(
                        id = c.getLong(idI),
                        address = c.getString(a).orEmpty(),
                        body = c.getString(b).orEmpty(),
                        date = c.getLong(d),
                        outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                            type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                            type == Telephony.Sms.MESSAGE_TYPE_FAILED
                    )
                }
            }
        }
    }

    private fun loadMmsConversation(
        context: Context,
        address: String,
        rows: MutableList<PolarSmsMessage>
    ) {
        runCatching {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX),
                null,
                null,
                "${Telephony.Mms.DATE} ASC"
            )?.use { c ->
                val idI = c.getColumnIndex(Telephony.Mms._ID)
                val d = c.getColumnIndex(Telephony.Mms.DATE)
                val box = c.getColumnIndex(Telephony.Mms.MESSAGE_BOX)
                while (c.moveToNext()) {
                    val mmsId = c.getLong(idI)
                    if (mmsAddress(context, mmsId) != address) continue
                    val parts = mmsParts(context, mmsId)
                    val text = parts.firstOrNull { it.second?.startsWith("text/") == true }?.first.orEmpty()
                    val media = parts.firstOrNull { it.second?.startsWith("text/") != true }
                    rows += PolarSmsMessage(
                        id = mmsId,
                        address = address,
                        body = text.ifBlank { media?.first ?: "Attachment" },
                        date = c.getLong(d) * 1000L,
                        outgoing = c.getInt(box) == Telephony.Mms.MESSAGE_BOX_SENT ||
                            c.getInt(box) == Telephony.Mms.MESSAGE_BOX_OUTBOX ||
                            c.getInt(box) == Telephony.Mms.MESSAGE_BOX_FAILED,
                        attachmentUri = media?.third,
                        attachmentMime = media?.second
                    )
                }
            }
        }
    }

    private fun mmsAddress(context: Context, mmsId: Long): String? {
        val uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId)
        return runCatching {
            context.contentResolver.query(
                Uri.parse("$uri/addr"),
                arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
                null,
                null,
                null
            )?.use { c ->
                val addrI = c.getColumnIndex(Telephony.Mms.Addr.ADDRESS)
                val typeI = c.getColumnIndex(Telephony.Mms.Addr.TYPE)
                var found: String? = null
                while (c.moveToNext()) {
                    val type = c.getInt(typeI)
                    val addr = c.getString(addrI).orEmpty()
                    if (type == 151 && addr.isNotBlank() && addr != "insert-address-token") {
                        found = addr
                    }
                }
                found
            }
        }.getOrNull()
    }

    private fun mmsSnippet(context: Context, mmsId: Long): String {
        val parts = mmsParts(context, mmsId)
        val text = parts.firstOrNull { it.second?.startsWith("text/") == true }?.first
        if (!text.isNullOrBlank()) return text
        val mime = parts.firstOrNull { it.second?.startsWith("text/") != true }?.second
        return when {
            mime?.startsWith("image/") == true -> "Photo"
            mime != null -> "Attachment"
            else -> "MMS"
        }
    }

    /** text, mime, content uri */
    private fun mmsParts(context: Context, mmsId: Long): List<Triple<String, String?, Uri?>> {
        val uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId)
        return runCatching {
            context.contentResolver.query(
                Uri.parse("$uri/part"),
                arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.TEXT),
                null,
                null,
                null
            )?.use { c ->
                val idI = c.getColumnIndex(Telephony.Mms.Part._ID)
                val mimeI = c.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE)
                val textI = c.getColumnIndex(Telephony.Mms.Part.TEXT)
                buildList {
                    while (c.moveToNext()) {
                        val partId = c.getLong(idI)
                        val mime = c.getString(mimeI)
                        val text = c.getString(textI).orEmpty()
                        val partUri = ContentUris.withAppendedId(
                            Uri.parse("content://mms/part"),
                            partId
                        )
                        add(Triple(text, mime, partUri))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}
