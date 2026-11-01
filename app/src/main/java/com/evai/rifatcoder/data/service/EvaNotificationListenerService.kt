package com.evai.rifatcoder.data.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real system NotificationListenerService for EVA AI.
 * Grants genuine device-level notification awareness when authorized by user.
 */
class EvaNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activeNotificationCount = MutableStateFlow(0)
        val activeNotificationCount: StateFlow<Int> = _activeNotificationCount.asStateFlow()

        var instance: EvaNotificationListenerService? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isConnected.value = true
        updateCount()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateCount()
    }

    private fun updateCount() {
        try {
            val notifications = activeNotifications
            _activeNotificationCount.value = notifications?.size ?: 0
        } catch (_: Exception) {
            _activeNotificationCount.value = 0
        }
    }
}
