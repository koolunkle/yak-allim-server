package com.example.yakallim.ocr.service

import com.example.yakallim.notification.service.NotificationClient
import com.example.yakallim.ocr.dto.OcrResultResponse
import com.example.yakallim.ocr.engine.N8nOcrClient
import com.example.yakallim.ocr.model.OcrPipelineStep
import com.example.yakallim.ocr.model.PrescribedMedicine
import com.example.yakallim.ocr.repository.OcrJobRepository
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
    ocrProgressManager: ProgressManager,
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

    fun handleCallback(jobId: String, medicines: List<PrescribedMedicine>) {
        val response = OcrResultResponse(
            fileName = "n8n_ocr_$jobId",
            message = "복약 안내서 분석이 완료되었습니다.\n복약 지침을 확인해 보세요.",
            textBlocks = emptyList(),
            medicines = medicines
        )

        val transitionApplied = ocrJobRepository.updateToCompleted(jobId, response)

        val token = fcmTokenMap.remove(jobId)

        if (transitionApplied) {
            ocrProgressManager.publishProgress(jobId, OcrPipelineStep.COMPLETED, response.message)

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
