package com.asifrezan.notificationreader.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.asifrezan.notificationreader.R
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NotificationAccessHelper {

    private var isPromptShowing = false

    fun isEnabled(activity: Activity): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(activity)
            .contains(activity.packageName)
    }

    fun showPrompt(activity: Activity, onContinue: () -> Unit = {}) {
        if (isEnabled(activity)) {
            onContinue()
            return
        }

        if (isPromptShowing || activity.isFinishing) return

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_notification_access, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setPositiveButton("Open settings") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                onContinue()
            }
            .setNeutralButton("Fix restricted access") { _, _ ->
                openAppInfo(activity)
                onContinue()
            }
            .setNegativeButton("Not now") { _, _ ->
                onContinue()
            }
            .create()

        dialog.setOnDismissListener {
            isPromptShowing = false
        }
        isPromptShowing = true
        dialog.show()
    }

    private fun openAppInfo(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }
}
