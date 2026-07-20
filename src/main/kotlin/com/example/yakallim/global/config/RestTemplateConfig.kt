package com.example.yakallim.global.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.function.Supplier

@Configuration
class RestTemplateConfig {

    @Bean("n8nRestTemplate")
    fun n8nRestTemplate(builder: RestTemplateBuilder): RestTemplate {
        val factorySupplier = Supplier<ClientHttpRequestFactory> { SimpleClientHttpRequestFactory() }

        return builder
            .requestFactory(factorySupplier)
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(60))
            .build()
    }
}