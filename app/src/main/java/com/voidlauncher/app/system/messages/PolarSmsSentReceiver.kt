package com.voidlauncher.app.system.messages

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class PolarSmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(PolarSms.EXTRA_ROW_ID, -1L)
        if (id < 0L) return
        val isMms = intent.action == ACTION_MMS_SENT ||
            intent.getBooleanExtra(PolarSms.EXTRA_IS_MMS, false)
        val nextType = when (resultCode) {
            Activity.RESULT_OK -> if (isMms) {
                Telephony.Mms.MESSAGE_BOX_SENT
            } else {
                Telephony.Sms.MESSAGE_TYPE_SENT
            }
            else -> if (isMms) {
                Telephony.Mms.MESSAGE_BOX_FAILED
            } else {
                Telephony.Sms.MESSAGE_TYPE_FAILED
            }
        }
        PolarSms.updateMessageBox(context, id, isMms, nextType)
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "SMS/MMS send failed result=$resultCode id=$id mms=$isMms")
        }
        PolarSms.emitChange()
    }

    companion object {
        const val ACTION_SMS_SENT = "com.voidlauncher.app.SMS_SENT"
        const val ACTION_MMS_SENT = "com.voidlauncher.app.MMS_SENT"
        private const val TAG = "PolarSmsSent"
    }
}
