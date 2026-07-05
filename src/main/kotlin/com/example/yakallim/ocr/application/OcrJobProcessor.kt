package com.example.yakallim.ocr.application

import com.example.yakallim.notification.domain.NotificationClient
import com.example.yakallim.ocr.domain.engine.OcrEngine
import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.infrastructure.parser.PrescriptionParser
import com.example.yakallim.ocr.presentation.dto.OcrResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.nio.file.Files
import java.nio.file.Path

@Component
class OcrJobProcessor(
    private val ocrEngine: OcrEngine,
    private val ocrJobRepository: OcrJobRepository,
    private val prescriptionParser: PrescriptionParser,
    @param:Qualifier("FCM_CLIENT") private val notifier: NotificationClient
) {
    private val log = LoggerFactory.getLogger(OcrJobProcessor::class.java)

    @Async
    fun executeTask(
        jobId: String,
        path: Path,
        fileName: String,
        token: String?,
        delay: Long? = null
    ) {
        delay?.takeIf { it > 0 }?.let {
            try {
                Thread.sleep(it)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        if (ocrJobRepository.isCancelled(jobId)) {
            log.info("OCR 작업 시작 전 취소: 작업ID='{}'", jobId)
            return
        }

        ocrJobRepository.updateToProcessing(jobId)

        val stopwatch = StopWatch(jobId)

        try {
            check(!ocrJobRepository.isCancelled(jobId)) { "ONNX 추론 전 취소됨" }

            stopwatch.start("ONNX 추론")
            val textBlocks = Files.newInputStream(path).use { ocrEngine.runOcr(it) }
            stopwatch.stop()

            check(!ocrJobRepository.isCancelled(jobId)) { "구조화 파싱 전 취소됨" }

            stopwatch.start("구조화 파싱")
            val prescriptions = prescriptionParser.parse(textBlocks)
            stopwatch.stop()

            log.info("\n${stopwatch.prettyPrint()}")

            val response = OcrResponse(
                fileName = fileName,
                message = "복약 안내서 분석이 완료되었습니다.\n복약 지침을 확인해 보세요.",
                textBlocks = textBlocks,
                prescriptions = prescriptions
            )

            ocrJobRepository.updateToCompleted(jobId, response)

            log.info("복약 안내서 분석 완료: {}", fileName)
            prescriptions.forEachIndexed { idx, item ->
                log.info(
                    "  [{}] 약품명: '{}', 복용량: '{}', 하루 횟수: '{}회', 복용 기간: '{}일'",
                    idx, item.medicineName, item.dosagePerTake, item.dailyFrequency, item.durationDays
                )
            }

            check(!ocrJobRepository.isCancelled(jobId)) { "알림 전송 전 취소됨" }

            notifier.notify(
                token = token ?: "",
                title = "복약 안내서 분석 완료",
                body = response.message,
                data = mapOf("jobId" to jobId, "status" to "COMPLETED", "message" to response.message)
            )
        } catch (e: IllegalStateException) {
            log.info("OCR 작업 취소: 작업ID='{}', 사유='{}'", jobId, e.message)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "알 수 없는 오류가 발생했습니다."
            log.error("비동기 OCR 처리 실패: {}", fileName, e)
            ocrJobRepository.updateToFailed(jobId, errorMessage)
            notifier.notify(
                token = token ?: "",
                title = "복약 안내서 분석 실패",
                body = errorMessage,
                data = mapOf("jobId" to jobId, "status" to "FAILED", "error" to errorMessage)
            )
        }
    }
}
