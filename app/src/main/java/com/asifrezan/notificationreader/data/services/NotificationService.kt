package com.asifrezan.notificationreader.data.services

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.asifrezan.notificationreader.data.models.Messages
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class NotificationService : NotificationListenerService() {

    private lateinit var database: DatabaseReference
    private lateinit var statusDatabase: DatabaseReference
    private var auth: FirebaseAuth? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        syncActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        syncActiveNotifications(sbn)
    }

    private fun syncActiveNotifications(newNotification: StatusBarNotification? = null) {
        val userId = getUserId() ?: return
        ensureDatabase()
        writeStatus(userId, "listener_active")

        val notifications = buildList {
            newNotification?.let { add(it) }
            activeNotifications?.forEach { active ->
                if (newNotification?.key != active.key) add(active)
            }
        }

        val updates = linkedMapOf<String, Any>()
        notifications.forEach { notification ->
            updates.putAll(createMessageUpdates(userId, notification))
        }

        if (updates.isNotEmpty()) {
            database.updateChildren(updates)
                .addOnSuccessListener {
                    writeStatus(userId, "stored_${updates.size}_messages")
                }
                .addOnFailureListener { error ->
                    writeStatus(userId, "batch_failed_${error.javaClass.simpleName}")
                    fallbackPushMessages(userId, notifications)
                }
        } else if (newNotification != null) {
            writeStatus(userId, "no_text_found_${newNotification.packageName}")
        }
    }

    private fun getUserId(): String? {
        if (auth == null) auth = FirebaseAuth.getInstance()
        return auth?.currentUser?.uid
            ?: PreferenceUtils.getString(this, PreferenceUtils.USER_ID_KEY)
                .takeIf { it.isNotBlank() }
    }

    private fun ensureDatabase() {
        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance().getReference("Messages")
        }
        if (!::statusDatabase.isInitialized) {
            statusDatabase = FirebaseDatabase.getInstance().getReference("DeviceStatus")
        }
    }

    private fun createMessageUpdates(userId: String, sbn: StatusBarNotification): Map<String, Any> {
        val packageName = sbn.packageName
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        val platform = when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("facebook.orca") || packageName.contains("messenger") -> "Messenger"
            packageName.contains("facebook.lite") || packageName.contains("facebook.katana") -> "Facebook"
            packageName.contains("imo") -> "Imo"
            else -> "Other"
        }

        if (platform == "Messenger" && title == "Chat heads active") return emptyMap()

        val capturedMessages = extractNotificationMessages(sbn.notification, title, sbn.postTime)
            .filterNot { it.message == "Checking for new messages" }

        if (capturedMessages.isEmpty()) return emptyMap()

        return capturedMessages.associate { capturedMessage ->
            val contactName = capturedMessage.contactName.ifBlank { title }
            val messageId = createStableMessageId(packageName, platform, contactName, capturedMessage)
            val messageData = Messages(
                userId = userId,
                contactName = contactName,
                timestamp = capturedMessage.timestamp.toString(),
                platform = platform,
                message = capturedMessage.message
            )

            "$userId/$platform/$messageId" to messageData
        }
    }

    private fun fallbackPushMessages(userId: String, notifications: List<StatusBarNotification>) {
        notifications.forEach { notification ->
            createMessageUpdates(userId, notification).values.forEach { messageData ->
                val message = messageData as? Messages ?: return@forEach
                database.child(userId).child(message.platform).push().setValue(message)
            }
        }
    }

    private fun writeStatus(userId: String, status: String) {
        if (!::statusDatabase.isInitialized) return

        statusDatabase.child(userId).updateChildren(
            mapOf(
                "lastStatus" to status,
                "lastSeenAt" to System.currentTimeMillis()
            )
        )
    }

    private fun extractNotificationMessages(
        notification: Notification,
        fallbackTitle: String,
        fallbackTimestamp: Long
    ): List<CapturedMessage> {
        val extras = notification.extras
        val messagingMessages = extras
            .getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapIndexedNotNull { index, item ->
                val bundle = item as? Bundle ?: return@mapIndexedNotNull null
                val text = bundle.getCharSequence("text")?.toString()?.trim().orEmpty()
                if (text.isBlank()) return@mapIndexedNotNull null

                val sender = bundle.getCharSequence("sender")?.toString()?.trim().orEmpty()
                val time = bundle.getLong("time", fallbackTimestamp + index)
                CapturedMessage(sender.ifBlank { fallbackTitle }, text, time)
            }
            .orEmpty()

        if (messagingMessages.isNotEmpty()) return messagingMessages

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapIndexedNotNull { index, line ->
                val text = line.toString().trim()
                if (text.isBlank()) null else CapturedMessage(fallbackTitle, text, fallbackTimestamp + index)
            }
            .orEmpty()

        if (lines.isNotEmpty()) return lines

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        if (bigText.isNotBlank()) return listOf(CapturedMessage(fallbackTitle, bigText.trim(), fallbackTimestamp))

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        return if (text.isNotBlank()) {
            listOf(CapturedMessage(fallbackTitle, text.trim(), fallbackTimestamp))
        } else {
            emptyList()
        }
    }

    private fun createStableMessageId(
        packageName: String,
        platform: String,
        contactName: String,
        message: CapturedMessage
    ): String {
        return sha256("$packageName|$platform|$contactName|${message.timestamp}|${message.message}")
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    private data class CapturedMessage(
        val contactName: String,
        val message: String,
        val timestamp: Long
    )
}
