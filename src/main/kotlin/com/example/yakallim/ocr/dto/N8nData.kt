package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.PrescribedMedicine

data class N8nData(
    val medicines: List<PrescribedMedicine> = emptyList()
)
