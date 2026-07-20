package com.example.yakallim.ocr.application

import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
@ConditionalOnProperty(name = ["ocr.type"], havingValue = "local", matchIfMissing = true)
class LocalOcrService(
    private val ocrJobProcessor: OcrJobProcessor,
    ocrJobRepository: OcrJobRepository,
    ocrProgressManager: OcrProgressManager,
    @Value("\${ocr.upload-dir:outputs/api-images}") uploadDirStr: String
) : OcrService(ocrJobRepository, ocrProgressManager, uploadDirStr) {

    override fun processJob(
        jobId: String,
        targetPath: Path,
        uniqueFileName: String,
        fcmToken: String?,
        delay: Long?
    ) {
        ocrJobProcessor.executeTask(jobId, targetPath, uniqueFileName, fcmToken, delay)
    }
}
