package com.example.yakallim.ocr.application

import com.example.yakallim.ocr.domain.exception.OcrException
import com.example.yakallim.ocr.domain.model.PipelineStep
import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class OcrService(
    private val ocrJobProcessor: OcrJobProcessor,
    private val ocrJobRepository: OcrJobRepository,
    private val ocrProgressManager: OcrProgressManager
) {
    private val log = LoggerFactory.getLogger(OcrService::class.java)
    private val uploadDir = Paths.get("outputs", "api-images").toAbsolutePath().normalize()

    init {
        try {
            Files.createDirectories(uploadDir)
            log.info("OCR 이미지 디렉터리 생성 완료: $uploadDir")
        } catch (e: IOException) {
            log.error("OCR 이미지 디렉터리 생성 실패: $uploadDir", e)
            throw OcrException.FileSaveException("OCR 이미지 디렉터리를 생성할 수 없습니다.", e)
        }
    }

    fun enqueueJob(
        file: MultipartFile,
        fcmToken: String?,
        delay: Long? = null
    ): OcrJobResponse {
        if (file.isEmpty) {
            throw OcrException.EmptyFileException()
        }

        val originalFilename = file.originalFilename ?: ""
        val extension = originalFilename.substringAfterLast('.', "").let { if (it.isNotEmpty()) ".$it" else "" }
        val uniqueFileName = "${UUID.randomUUID()}$extension"
        val targetPath = uploadDir.resolve(uniqueFileName)

        val startTime = System.currentTimeMillis()

        try {
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
            log.info("이미지 파일 저장 완료: $targetPath (소요 시간: {}ms)", System.currentTimeMillis() - startTime)
        } catch (e: IOException) {
            log.error("이미지 파일 저장 중 오류 발생: $targetPath", e)
            throw OcrException.FileSaveException("이미지 파일 저장 중 입출력 오류가 발생했습니다.", e)
        }

        val jobId = UUID.randomUUID().toString()
        val job = ocrJobRepository.registerJob(jobId)

        ocrProgressManager.publishProgress(jobId, PipelineStep.ENQUEUED)

        ocrJobProcessor.executeTask(jobId, targetPath, uniqueFileName, fcmToken, delay)

        return job
    }

    fun getJob(jobId: String): OcrJobResponse? = ocrJobRepository.getJob(jobId)

    fun cancelJob(jobId: String) {
        val job = ocrJobRepository.getJob(jobId)
            ?: throw OcrException.JobNotFoundException("존재하지 않는 작업ID: $jobId")

        if (job.status == OcrJobResponse.JobStatus.COMPLETED || job.status == OcrJobResponse.JobStatus.FAILED) {
            throw OcrException.IllegalJobStateException("이미 종료된 작업은 취소할 수 없습니다. (작업ID: $jobId, 상태: ${job.status})")
        }

        ocrJobRepository.updateToCancelled(jobId)
        log.info("OCR 작업 취소 요청 완료: 작업ID='{}' (이전 상태: {})", jobId, job.status)
    }
}
