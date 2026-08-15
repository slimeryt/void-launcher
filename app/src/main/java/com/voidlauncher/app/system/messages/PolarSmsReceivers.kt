package com.voidlauncher.app.system.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

class PolarSmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages: Array<SmsMessage> = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val from = messages.first().displayOriginatingAddress.orEmpty()
        PolarSms.insertInbox(context, from, body)
    }
}

class PolarMmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        PolarSms.emitChange()
    }
}
