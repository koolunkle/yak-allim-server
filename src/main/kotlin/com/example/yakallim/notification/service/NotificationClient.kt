package com.example.yakallim.notification.service

interface NotificationClient {
    fun notify(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    )
}
