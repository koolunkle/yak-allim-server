package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.OcrPipelineStep

data class OcrProgressResponse(
    val step: OcrPipelineStep? = null,
    val message: String? = null,
    val progress: Int? = null,
    val isFinished: Boolean = false
)
