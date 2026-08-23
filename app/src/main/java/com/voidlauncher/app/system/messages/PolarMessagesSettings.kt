package com.voidlauncher.app.system.messages

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.messagesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "polar_messages_prefs"
)

object PolarMessagesSettings {
    private val WifiSending = booleanPreferencesKey("wifi_sending")

    const val WIFI_SENDING_DISCLAIMER =
        "When enabled, Polar will try to send SMS and MMS while you are connected to Wi-Fi, " +
            "using your carrier's Wi-Fi Calling and SMS-over-IMS when your phone and plan support it.\n\n" +
            "Delivery still depends on your carrier and device. Polar does not send iMessage, " +
            "RCS, or other internet chat services over Wi-Fi. MMS attachments may still require " +
            "mobile data on some carriers even with this turned on."

    fun wifiSendingFlow(context: Context): Flow<Boolean> =
        context.messagesDataStore.data.map { prefs -> prefs[WifiSending] ?: false }

    fun wifiSendingEnabled(context: Context): Boolean = runBlocking {
        wifiSendingFlow(context).first()
    }

    suspend fun setWifiSending(context: Context, enabled: Boolean) {
        context.messagesDataStore.edit { it[WifiSending] = enabled }
    }
}
