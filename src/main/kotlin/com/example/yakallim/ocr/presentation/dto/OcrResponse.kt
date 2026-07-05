package com.example.yakallim.ocr.presentation.dto

import com.example.yakallim.ocr.domain.model.TextBlock

data class OcrResponse(
    val fileName: String,
    val message: String,
    val textBlocks: List<TextBlock> = emptyList(),
    val prescriptions: List<Prescription> = emptyList()
)
