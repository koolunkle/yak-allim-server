package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.OcrJobStatus

data class OcrJobResponse(
    val jobId: String,
    val status: OcrJobStatus,
    val result: OcrResponse? = null,
    val error: String? = null
)