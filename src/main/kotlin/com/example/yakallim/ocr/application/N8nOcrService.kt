package com.example.yakallim.ocr.application

import com.example.yakallim.notification.domain.NotificationClient
import com.example.yakallim.ocr.domain.model.PipelineStep
import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.infrastructure.client.N8nOcrClient
import com.example.yakallim.ocr.presentation.dto.OcrResponse
import com.example.yakallim.ocr.presentation.dto.Prescription
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service
@ConditionalOnProperty(name = ["ocr.type"], havingValue = "n8n")
class N8nOcrService(
    ocrJobRepository: OcrJobRepository,
    ocrProgressManager: OcrProgressManager,
    private val n8nOcrClient: N8nOcrClient,
    @param:Qualifier("FCM_CLIENT") private val notifier: NotificationClient,
    @Value("\${ocr.upload-dir:outputs/api-images}") uploadDirStr: String
) : OcrService(ocrJobRepository, ocrProgressManager, uploadDirStr) {

    private val fcmTokenMap = ConcurrentHashMap<String, String>()

    override fun processJob(
        jobId: String,
        targetPath: Path,
        uniqueFileName: String,
        fcmToken: String?,
        delay: Long?
    ) {
        if (fcmToken != null) {
            fcmTokenMap[jobId] = fcmToken
        }
        n8nOcrClient.sendToN8nAsync(jobId, targetPath.toFile(), fcmToken) { failedJobId ->
            fcmTokenMap.remove(failedJobId)
        }
    }

    fun handleCallback(jobId: String, prescriptions: List<Prescription>) {
        val response = OcrResponse(
            fileName = "n8n_ocr_$jobId",
            message = "복약 안내서 분석이 완료되었습니다.\n복약 지침을 확인해 보세요.",
            textBlocks = emptyList(),
            prescriptions = prescriptions
        )

        val transitionApplied = ocrJobRepository.updateToCompleted(jobId, response)

        if (transitionApplied) {
            ocrProgressManager.publishProgress(jobId, PipelineStep.COMPLETED, response.message)

            val token = fcmTokenMap.remove(jobId)
            if (!token.isNullOrEmpty()) {
                notifier.notify(
                    token = token,
                    title = "복약 안내서 분석 완료",
                    body = response.message,
                    data = mapOf("jobId" to jobId, "status" to "COMPLETED", "message" to response.message)
                )
            }
        }
    }
}
