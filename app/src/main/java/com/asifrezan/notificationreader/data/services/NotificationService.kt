package com.asifrezan.notificationreader.data.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.asifrezan.notificationreader.data.models.Messages
import com.asifrezan.notificationreader.utils.PreferenceUtils
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    private var previousNotificationText: String? = null
    private lateinit var database: DatabaseReference

    companion object {
        var notificationText: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val timeStamp = (System.currentTimeMillis()).toString()
        var msgFrom: String? = null
        val username = PreferenceUtils.getString(this, "username", "")

        if (!::database.isInitialized) {
            database = FirebaseDatabase.getInstance().getReference("Messages")
        }

        CoroutineScope(Dispatchers.IO).launch {

            when (packageName) {
                "com.whatsapp" -> {
                    Log.e("eeee", "WhatsApp notification: $title: $text" ?: "null")
                    msgFrom = "Whatsapp"
                    val messages = Messages(username!!, timeStamp, msgFrom!!, text!!)
                    database.child(title!!).child(text).setValue(messages).addOnSuccessListener {
                        Log.e("eeee", "Successfully saved in database")
                    }.addOnFailureListener {
                        Log.e("eeee", it.message.toString())
                    }

                }

                "com.facebook.lite" -> {
                    Log.e("eeee", "Facebook Lite notification: $title: $text" ?: "null")
                    msgFrom = "Facebook Lite"
                    val messages = Messages(username!!, timeStamp, msgFrom!!, text!!)
                    database.child(title!!).child(text).setValue(messages).addOnSuccessListener {
                        Log.e("eeee", "Successfully saved in database")
                    }.addOnFailureListener {
                        Log.e("eeee", it.message.toString())
                    }
                }

                "com.facebook.orca" -> {
                    if (title != "Chat heads active") {
                        Log.e("eeee", "Facebook Messenger notification: $title: $text" ?: "null")
                        msgFrom = "Facebook"
                        val messages = Messages(username!!, timeStamp, msgFrom!!, text!!)
                        database.child(title!!).child(text).setValue(messages)
                            .addOnSuccessListener {
                                Log.e("eeee", "Successfully saved in database")
                            }.addOnFailureListener {
                            Log.e("eeee", it.message.toString())
                        }
                    }

                }

                else -> {

                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }


}