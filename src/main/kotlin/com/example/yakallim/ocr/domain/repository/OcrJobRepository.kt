package com.example.yakallim.ocr.domain.repository

import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import com.example.yakallim.ocr.presentation.dto.OcrResponse

interface OcrJobRepository {
    fun registerJob(jobId: String): OcrJobResponse
    fun updateToProcessing(jobId: String)
    fun updateToCompleted(jobId: String, result: OcrResponse)
    fun updateToFailed(jobId: String, errorMessage: String)
    fun updateToCancelled(jobId: String)
    fun getJob(jobId: String): OcrJobResponse?
    fun isCancelled(jobId: String): Boolean
}