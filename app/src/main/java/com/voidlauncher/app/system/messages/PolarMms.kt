package com.voidlauncher.app.system.messages

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.voidlauncher.app.system.PolarRoles
import java.io.ByteArrayOutputStream

object PolarMms {
    private const val ADDR_TYPE_TO = 151
    private const val ADDR_TYPE_FROM = 137
    private const val MMS_TYPE_SEND_REQ = 128

    fun send(
        context: Context,
        address: String,
        attachmentUri: Uri,
        mimeType: String,
        text: String = "",
        wifiSending: Boolean = false
    ): Boolean {
        if (address.isBlank()) return false
        if (!PolarRoles.isMessages(context)) return false
        when (PolarMessagesNetwork.sendBlockReason(context, wifiSending, isMms = true)) {
            PolarSendNetworkBlock.WifiOnlyDisabled,
            PolarSendNetworkBlock.NoConnection -> return false
            null -> Unit
        }
        val sms = PolarSms.smsManager(context)
        return runCatching {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            val mmsUri = createMmsRecord(context, threadId) ?: return false
            val mmsId = mmsUri.lastPathSegment?.toLongOrNull() ?: return false
            insertAddress(context, mmsUri, "insert-address-token", ADDR_TYPE_FROM)
            insertAddress(context, mmsUri, address, ADDR_TYPE_TO)
            if (text.isNotBlank()) insertTextPart(context, mmsUri, text)
            insertAttachmentPart(context, mmsUri, attachmentUri, mimeType)
            val sentIntent = PolarSms.pendingSentIntent(context, mmsId, isMms = true)
            sms.sendMultimediaMessage(context, mmsUri, null, null, sentIntent)
            PolarSms.emitChange()
            true
        }.getOrDefault(false)
    }

    private fun createMmsRecord(context: Context, threadId: Long): Uri? {
        val values = ContentValues().apply {
            put(Telephony.Mms.THREAD_ID, threadId)
            put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
            put(Telephony.Mms.READ, 1)
            put(Telephony.Mms.MESSAGE_TYPE, MMS_TYPE_SEND_REQ)
            put(Telephony.Mms.MMS_VERSION, 18)
            put(Telephony.Mms.PRIORITY, 129)
            put(Telephony.Mms.READ_REPORT, 0)
            put(Telephony.Mms.MESSAGE_CLASS, "personal")
        }
        return context.contentResolver.insert(Telephony.Mms.CONTENT_URI, values)
    }

    private fun insertAddress(context: Context, mmsUri: Uri, address: String, type: Int) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Addr.ADDRESS, address)
            put(Telephony.Mms.Addr.CHARSET, 106)
            put(Telephony.Mms.Addr.TYPE, type)
        }
        context.contentResolver.insert(Uri.parse("$mmsUri/addr"), values)
    }

    private fun insertTextPart(context: Context, mmsUri: Uri, text: String) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain")
            put(Telephony.Mms.Part.CHARSET, 106)
            put(Telephony.Mms.Part.TEXT, text)
        }
        context.contentResolver.insert(Uri.parse("$mmsUri/part"), values)
    }

    private fun insertAttachmentPart(
        context: Context,
        mmsUri: Uri,
        attachmentUri: Uri,
        mimeType: String
    ) {
        val bytes = context.contentResolver.openInputStream(attachmentUri)?.use { input ->
            ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
        } ?: return
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.CONTENT_TYPE, mimeType)
            put(Telephony.Mms.Part.NAME, attachmentUri.lastPathSegment ?: "attachment")
        }
        val partUri = context.contentResolver.insert(Uri.parse("$mmsUri/part"), values) ?: return
        context.contentResolver.openOutputStream(partUri)?.use { it.write(bytes) }
    }
}
