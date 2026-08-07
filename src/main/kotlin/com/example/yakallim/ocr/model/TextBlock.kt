package com.example.yakallim.ocr.model

data class TextBlock(
    val text: String,
    val confidence: Float,
    val bounds: List<Point> = emptyList()
)

