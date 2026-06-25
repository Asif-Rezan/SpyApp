package com.asifrezan.notificationreader.data.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.asifrezan.notificationreader.data.models.Messages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NotificationService : NotificationListenerService() {

    private lateinit var database: DatabaseReference
    private var auth: FirebaseAuth? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val timeStamp = System.currentTimeMillis().toString()
        
        if (text.isEmpty() || text == "Checking for new messages") return

        if (auth == null) auth = FirebaseAuth.getInstance()
        val userId = auth?.currentUser?.uid ?: return

        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance().getReference("Messages")
        }

        val platform = when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("facebook.orca") || packageName.contains("messenger") -> "Messenger"
            packageName.contains("facebook.lite") || packageName.contains("facebook.katana") -> "Facebook"
            packageName.contains("imo") -> "Imo"
            else -> "Other"
        }

        if (platform == "Messenger" && title == "Chat heads active") return

        val messageData = Messages(userId, title, timeStamp, platform, text)
        
        // Push message to database: Messages / userId / platform / messageId
        val msgRef = database.child(userId).child(platform).push()
        msgRef.setValue(messageData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
