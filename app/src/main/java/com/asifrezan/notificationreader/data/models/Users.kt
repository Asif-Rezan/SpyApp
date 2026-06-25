package com.asifrezan.notificationreader.data.models

data class Users(
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: String = "",
    val subscriptionStatus: String = "free",
    val subscriptionPlan: String = "",
    val subscriptionStartedAt: Long = 0L,
    val subscriptionExpiresAt: Long = 0L
)
