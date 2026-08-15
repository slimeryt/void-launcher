package com.voidlauncher.app.system.callerid

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import com.voidlauncher.app.system.phone.PolarContacts

class PolarCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val contact = PolarContacts.lookupName(this, number)
        PolarCallerIdStore.add(
            this,
            number = PhoneNumberUtils.formatNumber(number, java.util.Locale.getDefault().country) ?: number,
            name = contact ?: number.ifBlank { "Unknown" },
            known = contact != null
        )
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )
    }
}
