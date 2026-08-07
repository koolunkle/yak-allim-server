package com.example.yakallim.global.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KtorClientConfig {

    private lateinit var client: HttpClient

    @Bean("n8nKtorClient")
    fun n8nKtorClient(): HttpClient {
        client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
            }
        }
        return client
    }

    @PreDestroy
    fun close() {
        if (::client.isInitialized) {
            client.close()
        }
    }
}
