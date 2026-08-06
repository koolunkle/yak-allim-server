package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.PrescribedMedicine
import com.example.yakallim.ocr.model.TextBlock

data class OcrResultResponse(
    val fileName: String,
    val message: String,
    val textBlocks: List<TextBlock> = emptyList(),
    val medicines: List<PrescribedMedicine> = emptyList()
)
