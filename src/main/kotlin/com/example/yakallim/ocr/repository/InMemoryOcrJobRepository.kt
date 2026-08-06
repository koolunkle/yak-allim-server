package com.example.yakallim.ocr.repository

import com.example.yakallim.ocr.dto.OcrJobResponse
import com.example.yakallim.ocr.dto.OcrResultResponse
import com.example.yakallim.ocr.model.OcrJobStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryOcrJobRepository : OcrJobRepository {

    private val jobRegistry = ConcurrentHashMap<String, OcrJobResponse>()

    override fun registerJob(jobId: String): OcrJobResponse =
        OcrJobResponse(jobId = jobId, status = OcrJobStatus.ACCEPTED).also {
            jobRegistry[jobId] = it
        }

    override fun updateToProcessing(jobId: String) {
        updateJobStatus(jobId, OcrJobStatus.PROCESSING)
    }

    override fun updateToCompleted(jobId: String, result: OcrResultResponse): Boolean {
        var transitionApplied = false
        jobRegistry.computeIfPresent(jobId) { _, existing ->
            if (existing.status == OcrJobStatus.ACCEPTED || existing.status == OcrJobStatus.PROCESSING) {
                transitionApplied = true
                existing.copy(status = OcrJobStatus.COMPLETED, result = result)
            } else {
                transitionApplied = false
                existing
            }
        }
        return transitionApplied
    }

    override fun updateToFailed(jobId: String, errorMessage: String) {
        updateJobStatus(jobId, OcrJobStatus.FAILED, error = errorMessage)
    }

    override fun updateToCancelled(jobId: String) {
        updateJobStatus(jobId, OcrJobStatus.CANCELLED)
    }

    override fun getJob(jobId: String): OcrJobResponse? = jobRegistry[jobId]

    override fun isCancelled(jobId: String): Boolean = jobRegistry[jobId]?.status == OcrJobStatus.CANCELLED

    private fun updateJobStatus(
        jobId: String, status: OcrJobStatus, result: OcrResultResponse? = null, error: String? = null
    ): Boolean {
        var transitionApplied = false
        jobRegistry.compute(jobId) { _, existing ->
            if (existing?.status == OcrJobStatus.CANCELLED && status != OcrJobStatus.CANCELLED) {
                transitionApplied = false
                return@compute existing
            }
            transitionApplied = true
            existing?.copy(
                status = status, result = result ?: existing.result, error = error
            ) ?: OcrJobResponse(
                jobId = jobId, status = status, result = result, error = error
            )
        }
        return transitionApplied
    }
}