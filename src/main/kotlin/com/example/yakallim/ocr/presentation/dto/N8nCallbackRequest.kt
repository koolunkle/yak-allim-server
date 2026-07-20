package com.example.yakallim.ocr.presentation.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class N8nCallbackRequest(
    @field:NotBlank(message = "jobId는 필수 항목입니다.")
    val jobId: String?,
    val status: String?,
    @field:Valid
    val data: N8nData
) {
    data class N8nData(
        val prescriptions: List<Prescription> = emptyList()
    )
}
