package com.example.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationCache {
    private const val MAX_ITEMS = 20
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun addOrUpdate(item: NotificationItem) {
        val current = _notifications.value.filterNot { it.id == item.id }
        _notifications.value = (listOf(item) + current).take(MAX_ITEMS)
    }

    fun removeById(id: String) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    fun isNotificationAccessEnabled(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun summarizeForPrompt(): String {
        val list = _notifications.value
        if (list.isEmpty()) return "No unread notifications captured yet."
        return list.take(8).joinToString(separator = "\n") { item ->
            "- [${item.appName}] ${item.title}: ${item.body}".take(180)
        }
    }
}

class HayatNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        NotificationCache.addOrUpdate(
            NotificationItem(
                id = sbn.key ?: "${sbn.packageName}_${sbn.id}",
                appName = appName,
                packageName = sbn.packageName,
                title = title.ifBlank { appName },
                body = text.ifBlank { "New notification" },
                postedAtMillis = sbn.postTime
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        NotificationCache.removeById(key)
    }
}
