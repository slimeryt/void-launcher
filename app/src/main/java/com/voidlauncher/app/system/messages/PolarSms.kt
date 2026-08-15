package com.voidlauncher.app.system.messages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
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
    val outgoing: Boolean
)

object PolarSms {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emitChange() {
        changes.tryEmit(Unit)
    }

    fun canRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun threads(context: Context): List<PolarSmsThread> {
        if (!canRead(context)) return emptyList()
        val grouped = linkedMapOf<String, PolarSmsThread>()
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
                    if (address.isBlank() || address in grouped) continue
                    grouped[address] = PolarSmsThread(
                        address = address,
                        snippet = c.getString(b).orEmpty(),
                        date = c.getLong(d)
                    )
                }
            }
        }
        return grouped.values.toList()
    }

    fun conversation(context: Context, address: String): List<PolarSmsMessage> {
        if (!canRead(context)) return emptyList()
        val rows = mutableListOf<PolarSmsMessage>()
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
                    rows += PolarSmsMessage(
                        id = c.getLong(idI),
                        address = c.getString(a).orEmpty(),
                        body = c.getString(b).orEmpty(),
                        date = c.getLong(d),
                        outgoing = c.getInt(t) == Telephony.Sms.MESSAGE_TYPE_SENT ||
                            c.getInt(t) == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                    )
                }
            }
        }
        return rows
    }

    fun send(context: Context, address: String, body: String): Boolean {
        if (address.isBlank() || body.isBlank()) return false
        val sms = smsManager(context)
        return runCatching {
            val parts = sms.divideMessage(body)
            if (parts.size > 1) sms.sendMultipartTextMessage(address, null, parts, null, null)
            else sms.sendTextMessage(address, null, body, null, null)
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            runCatching { context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values) }
            emitChange()
            true
        }.getOrDefault(false)
    }

    fun insertInbox(context: Context, address: String, body: String) {
        val values = android.content.ContentValues().apply {
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
    private fun smsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }

}
