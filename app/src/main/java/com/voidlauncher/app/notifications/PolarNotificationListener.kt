package com.voidlauncher.app.notifications

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import android.provider.Settings

data class ShadeNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isClearable: Boolean,
    val appLabel: String,
    /** Loaded on the service thread; may be null. */
    @Transient val icon: Drawable? = null,
    @Transient val contentIntent: android.app.PendingIntent? = null
)

/**
 * In-process mirror of [PolarNotificationListener] for Compose.
 * Empty until the user grants notification access and the service connects.
 */
object NotificationMirror {
    private val _items = MutableStateFlow<List<ShadeNotification>>(emptyList())
    val items: StateFlow<List<ShadeNotification>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile
    private var serviceRef: WeakReference<PolarNotificationListener>? = null

    fun isAccessGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun openAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    internal fun attach(service: PolarNotificationListener) {
        serviceRef = WeakReference(service)
        _connected.value = true
        publish(service.activeNotifications?.toList().orEmpty())
    }

    internal fun detach(service: PolarNotificationListener) {
        if (serviceRef?.get() === service) {
            serviceRef = null
            _connected.value = false
            _items.value = emptyList()
        }
    }

    internal fun publish(raw: List<StatusBarNotification>) {
        val pm = serviceRef?.get()?.packageManager ?: return
        _items.value = raw
            .asSequence()
            .filter { !it.isOngoing || isUsefulOngoing(it) }
            .sortedByDescending { it.postTime }
            .map { sbn ->
                val n = sbn.notification
                val extras = n.extras
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                    .ifBlank {
                        extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString().orEmpty()
                    }
                val text = sequenceOf(
                    extras?.getCharSequence(Notification.EXTRA_TEXT),
                    extras?.getCharSequence(Notification.EXTRA_BIG_TEXT),
                    extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)
                ).mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
                    .firstOrNull()
                    .orEmpty()
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                }.getOrDefault(sbn.packageName)
                val icon = runCatching {
                    n.getLargeIcon()?.loadDrawable(serviceRef?.get())
                        ?: n.smallIcon?.loadDrawable(serviceRef?.get())
                }.getOrNull()
                ShadeNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title.ifBlank { label },
                    text = text,
                    postTime = sbn.postTime,
                    isClearable = sbn.isClearable,
                    appLabel = label,
                    icon = icon,
                    contentIntent = n.contentIntent
                )
            }
            .toList()
    }

    fun cancel(key: String) {
        serviceRef?.get()?.cancelNotification(key)
    }

    fun cancelAll() {
        serviceRef?.get()?.cancelAllNotifications()
    }

    private fun isUsefulOngoing(sbn: StatusBarNotification): Boolean {
        // Keep media / calls-ish ongoing; drop pure foreground service noise later if needed.
        val cat = sbn.notification.category
        return cat == Notification.CATEGORY_TRANSPORT ||
            cat == Notification.CATEGORY_CALL ||
            cat == Notification.CATEGORY_NAVIGATION
    }
}

class PolarNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationMirror.attach(this)
    }

    override fun onListenerDisconnected() {
        NotificationMirror.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishActive()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishActive()
    }

    private fun publishActive() {
        NotificationMirror.publish(activeNotifications?.toList().orEmpty())
    }
}
