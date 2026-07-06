package com.example.yakallim.ocr.presentation.dto

import com.example.yakallim.ocr.domain.model.PipelineStep
import com.fasterxml.jackson.annotation.JsonProperty

data class OcrProgressResponse(
    val step: PipelineStep,
    val message: String,
    val progress: Int,
    @get:JsonProperty("isFinished")
    val isFinished: Boolean
)
