package com.asifrezan.notificationreader.data.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    private var previousNotificationText: String? = null

    companion object {
        var notificationText: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        when (packageName) {
            "com.whatsapp" -> {
                // This is a notification from WhatsApp
                Log.e("eeee", "WhatsApp notification: $title: $text" ?: "null")
                // Show the notification text in your UI or perform any other action
            }
            "com.facebook.lite" -> {
                // This is a notification from Facebook Lite
                Log.e("eeee", "Facebook Lite notification: $title: $text" ?: "null")
                // Show the notification text in your UI or perform any other action
            }
            "com.facebook.orca" -> {
                // This is a notification from Facebook Messenger
                if (title != "Chat heads active")
                {
                    Log.e("eeee", "Facebook Messenger notification: $title: $text" ?: "null")
                }

                // Show the notification text in your UI or perform any other action
            }
            else -> {
                // This is a notification from another app
              //  Log.e("eeee", "Other notification: $title: $text" ?: "null")
            }

        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }



}