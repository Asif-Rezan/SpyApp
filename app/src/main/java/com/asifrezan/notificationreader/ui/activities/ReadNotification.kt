package com.asifrezan.notificationreader.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.asifrezan.notificationreader.R
import com.asifrezan.notificationreader.data.services.NotificationService

class ReadNotification : AppCompatActivity() {
    private lateinit var notificationService: NotificationService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_notification)


//        val notificationText = NotificationService.notificationText
//
//        Log.e("eeee", notificationText.toString())


    }
}