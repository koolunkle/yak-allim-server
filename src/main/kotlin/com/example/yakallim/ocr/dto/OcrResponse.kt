package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.TextBlock
import com.example.yakallim.ocr.model.PrescribedMedicine

data class OcrResponse(
    val fileName: String,
    val message: String,
    val textBlocks: List<TextBlock> = emptyList(),
    val medicines: List<PrescribedMedicine> = emptyList()
)
