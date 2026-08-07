package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.PipelineStep

data class OcrProgressResponse(
    val step: PipelineStep? = null,
    val message: String? = null,
    val progress: Int? = null,
    val isFinished: Boolean = false
)
