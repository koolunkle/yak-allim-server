package com.example.yakallim.ocr.presentation.dto

data class OcrJobResponse(
    val jobId: String,
    val status: JobStatus,
    val result: OcrResponse? = null,
    val error: String? = null
) {
    enum class JobStatus {
        ACCEPTED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}