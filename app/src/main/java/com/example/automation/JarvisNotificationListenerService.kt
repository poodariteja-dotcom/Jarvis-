package com.example.automation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSummary(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class JarvisNotificationListenerService : NotificationListenerService() {
    companion object {
        private var instance: JarvisNotificationListenerService? = null

        private val _recentNotifications = MutableStateFlow<List<NotificationSummary>>(emptyList())
        val recentNotifications: StateFlow<List<NotificationSummary>> = _recentNotifications.asStateFlow()

        private val _isListening = MutableStateFlow(false)
        val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

        fun isConnected(): Boolean = instance != null

        fun getLatestSummary(): String {
            val list = _recentNotifications.value
            if (list.isEmpty()) return "You have no unread notifications at present, Sir."
            val sb = StringBuilder("Here are your latest notifications, Sir:\n")
            list.take(5).forEachIndexed { idx, n ->
                sb.append("${idx + 1}. From ${n.appName}: ${n.title} - ${n.text}\n")
            }
            return sb.toString()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isListening.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
            _isListening.value = false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { addNotification(it) }
    }

    private fun refreshActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val summaries = active.mapNotNull { extractSummary(it) }
            _recentNotifications.value = summaries.take(20)
        } catch (_: Exception) {}
    }

    private fun addNotification(sbn: StatusBarNotification) {
        val summary = extractSummary(sbn) ?: return
        val current = _recentNotifications.value.toMutableList()
        current.removeAll { it.packageName == summary.packageName && it.title == summary.title }
        current.add(0, summary)
        if (current.size > 25) current.removeAt(current.lastIndex)
        _recentNotifications.value = current
    }

    private fun extractSummary(sbn: StatusBarNotification): NotificationSummary? {
        val extras = sbn.notification?.extras ?: return null
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return null

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        return NotificationSummary(
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            postTime = sbn.postTime
        )
    }
}
