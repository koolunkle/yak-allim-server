package com.example.yakallim.ocr.engine

import com.example.yakallim.notification.service.PushNotificationClient
import com.example.yakallim.ocr.config.OcrProperties
import com.example.yakallim.ocr.exception.OcrErrorMessageResolver
import com.example.yakallim.ocr.model.OcrPipelineStep
import com.example.yakallim.ocr.repository.OcrJobRepository
import com.example.yakallim.ocr.service.OcrProgressManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    @param:Qualifier("FCM_CLIENT") private val notifier: PushNotificationClient,
    private val ocrProperties: OcrProperties,
    @param:Qualifier("n8nRestTemplate") private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(N8nOcrClient::class.java)

    @Async
    fun sendToN8nAsync(jobId: String, file: File, fcmToken: String?, onDispatchFailure: (String) -> Unit) {
        try {
            ocrProgressManager.publishProgress(jobId, OcrPipelineStep.IMAGE_PROCESSING)

            val uri = UriComponentsBuilder.fromUriString(ocrProperties.n8n.webhookUrl)
                .queryParam("jobId", jobId)
                .build()
                .toUri()

            val headers = HttpHeaders()
            headers.contentType = MediaType.MULTIPART_FORM_DATA
            if (ocrProperties.n8n.webhookSecret.isNotBlank()) {
                headers.set("X-N8N-WEBHOOK-SECRET", ocrProperties.n8n.webhookSecret)
            }

            val body = LinkedMultiValueMap<String, Any>()

            val fileHeaders = HttpHeaders()
            fileHeaders.contentType = MediaType.APPLICATION_OCTET_STREAM
            fileHeaders.setContentDispositionFormData("file", file.name)
            val fileEntity = HttpEntity(FileSystemResource(file), fileHeaders)
            body.add("file", fileEntity)

            val requestEntity = HttpEntity(body, headers)

            ocrProgressManager.publishProgress(jobId, OcrPipelineStep.TEXT_DETECTION)
            restTemplate.postForEntity<String>(uri, requestEntity)

            ocrProgressManager.publishProgress(jobId, OcrPipelineStep.TEXT_RECOGNITION)
        } catch (e: Exception) {
            log.error("Failed to send image to n8n", e)
            val rawErrorMessage = e.message ?: "Failed to connect to n8n"
            val userFacingMessage = OcrErrorMessageResolver.resolve(e)
            ocrJobRepository.updateToFailed(jobId, rawErrorMessage)
            ocrProgressManager.publishProgress(jobId, OcrPipelineStep.FAILED, userFacingMessage)

            onDispatchFailure(jobId)

            if (!fcmToken.isNullOrEmpty()) {
                notifier.notify(
                    token = fcmToken,
                    title = "복약 안내서 분석 실패",
                    body = userFacingMessage,
                    data = mapOf(
                        "jobId" to jobId,
                        "status" to "FAILED",
                        "errorCode" to "N8N_DISPATCH_FAILED",
                        "message" to userFacingMessage
                    )
                )
            }
        }
    }
}
