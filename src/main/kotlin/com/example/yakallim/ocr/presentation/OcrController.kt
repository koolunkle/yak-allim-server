package com.example.yakallim.ocr.presentation

import com.example.yakallim.ocr.application.OcrProgressManager
import com.example.yakallim.ocr.application.OcrService
import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/ocr")
class OcrController(
    private val ocrService: OcrService,
    private val ocrProgressManager: OcrProgressManager
) {
    @PostMapping("/enqueue", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun enqueueJob(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("fcmToken", required = false) fcmToken: String?,
        @RequestParam("delay", required = false) delay: Long?
    ): ResponseEntity<OcrJobResponse> {
        val job = ocrService.enqueueJob(file, fcmToken, delay)
        return ResponseEntity.accepted().body(job)
    }

    @GetMapping("/jobs/{jobId}")
    fun getJob(@PathVariable jobId: String): ResponseEntity<OcrJobResponse> {
        val job = ocrService.getJob(jobId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/jobs/{jobId}/progress", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getJobProgress(
        @PathVariable jobId: String,
        response: HttpServletResponse
    ): SseEmitter {
        response.addHeader("X-Accel-Buffering", "no")
        response.addHeader("Cache-Control", "no-cache")
        return ocrProgressManager.registerEmitter(jobId)
    }

    @PostMapping("/jobs/{jobId}/cancel")
    fun cancelJob(@PathVariable jobId: String): ResponseEntity<Unit> {
        ocrService.cancelJob(jobId)
        return ResponseEntity.noContent().build()
    }
}
