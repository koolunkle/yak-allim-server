package com.example.yakallim.notification.infrastructure

import com.example.yakallim.notification.domain.NotificationClient
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile

@Component("FCM_CLIENT")
@Profile("!test")
class FirebaseMessagingNotificationClient(
    private val firebaseMessaging: FirebaseMessaging
) : NotificationClient {

    private val log = LoggerFactory.getLogger(FirebaseMessagingNotificationClient::class.java)

    override fun notify(token: String, title: String, body: String, data: Map<String, String>) {
        if (token.isBlank()) {
            log.warn("FCM 토큰 누락으로 인한 알림 전송 생략")
            return
        }

        runCatching {
            val notification = Notification.builder().setTitle(title).setBody(body).build()
            val message = Message.builder().apply {
                setToken(token)
                setNotification(notification)
                putAllData(data)
            }.build()

            firebaseMessaging.send(message)
        }.onSuccess {
            log.info("FCM 전송 완료: [제목] {}", title)
        }.onFailure { e ->
            log.error("FCM 전송 실패: [제목] {}, [토큰] {}", title, token, e)
        }
    }
}
