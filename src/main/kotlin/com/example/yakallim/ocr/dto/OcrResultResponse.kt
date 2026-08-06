package com.example.yakallim.ocr.dto

import com.example.yakallim.ocr.model.OcrTextBlock
import com.example.yakallim.ocr.model.PrescribedMedicine

data class OcrResultResponse(
    val fileName: String,
    val message: String,
    val textBlocks: List<OcrTextBlock> = emptyList(),
    val medicines: List<PrescribedMedicine> = emptyList()
)
