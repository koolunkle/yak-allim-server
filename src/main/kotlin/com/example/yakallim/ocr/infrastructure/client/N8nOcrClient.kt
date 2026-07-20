package com.example.yakallim.ocr.infrastructure.client

import com.example.yakallim.notification.domain.NotificationClient
import com.example.yakallim.ocr.application.OcrProgressManager
import com.example.yakallim.ocr.domain.model.PipelineStep
import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.io.File

@Component
class N8nOcrClient(
    private val ocrJobRepository: OcrJobRepository,
    private val ocrProgressManager: OcrProgressManager,
    @param:Qualifier("FCM_CLIENT") private val notifier: NotificationClient,
    @param:Value("\${ocr.n8n.webhook-url}") private val n8nWebhookUrl: String
) {
    private val log = LoggerFactory.getLogger(N8nOcrClient::class.java)
    private val restTemplate = RestTemplate()

    @Async
    fun sendToN8nAsync(jobId: String, file: File, fcmToken: String?) {
        try {
            ocrProgressManager.publishProgress(jobId, PipelineStep.IMAGE_PROCESSING)

            val uri = UriComponentsBuilder.fromUriString(n8nWebhookUrl)
                .queryParam("jobId", jobId)
                .build()
                .toUri()

            val headers = HttpHeaders()
            headers.contentType = MediaType.MULTIPART_FORM_DATA

            val body = LinkedMultiValueMap<String, Any>()

            val fileHeaders = HttpHeaders()
            fileHeaders.contentType = MediaType.APPLICATION_OCTET_STREAM
            fileHeaders.setContentDispositionFormData("file", file.name)
            val fileEntity = HttpEntity(FileSystemResource(file), fileHeaders)
            body.add("file", fileEntity)

            val requestEntity = HttpEntity(body, headers)

            ocrProgressManager.publishProgress(jobId, PipelineStep.TEXT_DETECTION)
            restTemplate.postForEntity<String>(uri, requestEntity)

            ocrProgressManager.publishProgress(jobId, PipelineStep.TEXT_RECOGNITION)
        } catch (e: Exception) {
            log.error("Failed to send image to n8n", e)
            val errorMessage = e.message ?: "Failed to connect to n8n"
            ocrJobRepository.updateToFailed(jobId, errorMessage)
            ocrProgressManager.publishProgress(jobId, PipelineStep.FAILED, errorMessage)

            if (!fcmToken.isNullOrEmpty()) {
                notifier.notify(
                    token = fcmToken,
                    title = "복약 안내서 분석 실패",
                    body = errorMessage,
                    data = mapOf("jobId" to jobId, "status" to "FAILED", "error" to errorMessage)
                )
            }
        }
    }
}
