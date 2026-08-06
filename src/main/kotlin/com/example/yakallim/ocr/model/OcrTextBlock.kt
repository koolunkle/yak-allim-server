package com.example.yakallim.ocr.model

data class OcrTextBlock(
    val text: String,
    val confidence: Float,
    val bounds: List<Coordinate> = emptyList()
) {
    data class Coordinate(val x: Int, val y: Int)
}
