package com.example.yakallim.notification.service

interface PushNotificationClient {
    fun notify(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    )
}
