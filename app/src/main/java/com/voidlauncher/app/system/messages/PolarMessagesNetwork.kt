package com.voidlauncher.app.system.messages

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager

enum class PolarSendNetworkBlock {
    WifiOnlyDisabled,
    NoConnection
}

object PolarMessagesNetwork {
    /** When Wi-Fi sending is off, block sends that would rely on Wi-Fi-only connectivity. */
    fun sendBlockReason(context: Context, wifiSending: Boolean, isMms: Boolean): PolarSendNetworkBlock? {
        if (wifiSending) return null
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return PolarSendNetworkBlock.NoConnection
        val active = cm.activeNetwork
        if (active == null) return PolarSendNetworkBlock.NoConnection
        val caps = cm.getNetworkCapabilities(active) ?: return PolarSendNetworkBlock.NoConnection
        val onWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val onCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        if (onCellular || hasCellularService(context)) return null
        if (onWifi) return PolarSendNetworkBlock.WifiOnlyDisabled
        if (isMms) return PolarSendNetworkBlock.WifiOnlyDisabled
        return PolarSendNetworkBlock.NoConnection
    }

    private fun hasCellularService(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return false
        return when (tm.dataState) {
            TelephonyManager.DATA_CONNECTED,
            TelephonyManager.DATA_CONNECTING -> true
            else -> if (Build.VERSION.SDK_INT >= 26) {
                tm.isDataEnabled && tm.simState == TelephonyManager.SIM_STATE_READY
            } else {
                tm.simState == TelephonyManager.SIM_STATE_READY
            }
        }
    }
}
