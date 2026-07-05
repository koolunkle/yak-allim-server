package com.example.yakallim.ocr.infrastructure.persistence

import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import com.example.yakallim.ocr.presentation.dto.OcrResponse
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryOcrJobRepository : OcrJobRepository {

    private val jobRegistry = ConcurrentHashMap<String, OcrJobResponse>()

    override fun registerJob(jobId: String): OcrJobResponse {
        val job = OcrJobResponse(jobId = jobId, status = OcrJobResponse.JobStatus.ACCEPTED)
        jobRegistry[jobId] = job
        return job
    }

    override fun updateToProcessing(jobId: String) {
        updateJobStatus(jobId, OcrJobResponse.JobStatus.PROCESSING)
    }

    override fun updateToCompleted(jobId: String, result: OcrResponse) {
        updateJobStatus(jobId, OcrJobResponse.JobStatus.COMPLETED, result = result)
    }

    override fun updateToFailed(jobId: String, errorMessage: String) {
        updateJobStatus(jobId, OcrJobResponse.JobStatus.FAILED, error = errorMessage)
    }

    override fun updateToCancelled(jobId: String) {
        updateJobStatus(jobId, OcrJobResponse.JobStatus.CANCELLED)
    }

    override fun getJob(jobId: String): OcrJobResponse? = jobRegistry[jobId]

    override fun isCancelled(jobId: String): Boolean = jobRegistry[jobId]?.status == OcrJobResponse.JobStatus.CANCELLED

    private fun updateJobStatus(
        jobId: String, status: OcrJobResponse.JobStatus, result: OcrResponse? = null, error: String? = null
    ) {
        jobRegistry.compute(jobId) { _, existing ->
            if (existing?.status == OcrJobResponse.JobStatus.CANCELLED && status != OcrJobResponse.JobStatus.CANCELLED) {
                return@compute existing
            }
            existing?.copy(
                status = status, result = result ?: existing.result, error = error
            ) ?: OcrJobResponse(
                jobId = jobId, status = status, result = result, error = error
            )
        }
    }
}