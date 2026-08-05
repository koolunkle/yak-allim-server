package com.example.yakallim.ocr.model

data class TextBlock(
    val text: String,
    val confidence: Float,
    val bounds: List<Coordinate>
) {
    data class Coordinate(val x: Int, val y: Int)
}
