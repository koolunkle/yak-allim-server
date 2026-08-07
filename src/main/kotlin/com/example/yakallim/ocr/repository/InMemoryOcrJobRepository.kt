package com.example.yakallim.ocr.repository

import com.example.yakallim.ocr.dto.OcrJobResponse
import com.example.yakallim.ocr.dto.OcrResponse
import com.example.yakallim.ocr.model.JobStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryOcrJobRepository : OcrJobRepository {

    private val jobRegistry = ConcurrentHashMap<String, OcrJobResponse>()

    override fun registerJob(jobId: String): OcrJobResponse =
        OcrJobResponse(jobId = jobId, status = JobStatus.ACCEPTED).also {
            jobRegistry[jobId] = it
        }

    override fun updateToProcessing(jobId: String) {
        updateJobStatus(jobId, JobStatus.PROCESSING)
    }

    override fun updateToCompleted(jobId: String, result: OcrResponse): Boolean {
        var transitionApplied = false
        jobRegistry.computeIfPresent(jobId) { _, existing ->
            if (existing.status == JobStatus.ACCEPTED || existing.status == JobStatus.PROCESSING) {
                transitionApplied = true
                existing.copy(status = JobStatus.COMPLETED, result = result)
            } else {
                transitionApplied = false
                existing
            }
        }
        return transitionApplied
    }

    override fun updateToFailed(jobId: String, errorMessage: String) {
        updateJobStatus(jobId, JobStatus.FAILED, error = errorMessage)
    }

    override fun updateToCancelled(jobId: String) {
        updateJobStatus(jobId, JobStatus.CANCELLED)
    }

    override fun getJob(jobId: String): OcrJobResponse? = jobRegistry[jobId]

    override fun isCancelled(jobId: String): Boolean = jobRegistry[jobId]?.status == JobStatus.CANCELLED

    private fun updateJobStatus(
        jobId: String, status: JobStatus, result: OcrResponse? = null, error: String? = null
    ): Boolean {
        var transitionApplied = false
        jobRegistry.compute(jobId) { _, existing ->
            if (existing?.status == JobStatus.CANCELLED && status != JobStatus.CANCELLED) {
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