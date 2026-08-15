package com.voidlauncher.app.system.messages

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder

class PolarHeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dest = intent?.data?.schemeSpecificPart?.let { Uri.decode(it) }.orEmpty()
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (dest.isNotBlank() && body.isNotBlank()) {
            PolarSms.send(this, dest, body)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
