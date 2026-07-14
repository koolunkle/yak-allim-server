package com.example.yakallim.notification.infrastructure.config

import com.example.yakallim.notification.domain.NotificationClient
import com.google.firebase.messaging.FirebaseMessaging
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class MockFirebaseConfig {

    /**
     * Provides a mocked `FirebaseMessaging` instance for tests.
     *
     * @return A mock `FirebaseMessaging` instance.
     */
    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        return Mockito.mock(FirebaseMessaging::class.java)
    }

    /**
     * Provides a no-op notification client for tests.
     *
     * @return A `NotificationClient` whose `notify` method does nothing.
     */
    @Bean("FCM_CLIENT")
    fun mockNotificationClient(): NotificationClient {
        return object : NotificationClient {
            override fun notify(token: String, title: String, body: String, data: Map<String, String>) {
                // CI 테스트 시 Firebase 호출을 모킹하여 무시
            }
        }
    }
}