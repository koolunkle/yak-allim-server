package com.example.yakallim.notification.infrastructure.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ResourceLoader

@Configuration
@Profile("!test")
class FirebaseMessagingConfig(
    private val resourceLoader: ResourceLoader
) {
    private val log = LoggerFactory.getLogger(FirebaseMessagingConfig::class.java)

    @Value("\${notification.firebase.key-path}")
    private lateinit var firebaseKeyPath: String

    @PostConstruct
    fun init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val resource = resourceLoader.getResource(firebaseKeyPath)
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.inputStream))
                    .build()
                FirebaseApp.initializeApp(options)
                log.info("Firebase Application 초기화 완료 (파일 경로: {})", firebaseKeyPath)
            }
        } catch (e: Exception) {
            log.error("Firebase Application 초기화 중 오류 발생 (파일 경로: {})", firebaseKeyPath, e)
        }
    }

    @Bean
    fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}