package com.asifrezan.notificationreader.utils

import android.content.Context

object PreferenceUtils {
    private const val PREFS_NAME = "SpyAppUser"
    const val USER_ID_KEY = "user_id"

    // Save data to SharedPreferences
    fun saveString(context: Context, key: String, value: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    // Get data from SharedPreferences
    fun getString(context: Context, key: String, defaultValue: String = ""): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun remove(context: Context, key: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(key).apply()
    }
}
