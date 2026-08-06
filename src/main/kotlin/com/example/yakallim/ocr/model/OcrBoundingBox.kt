package com.example.yakallim.ocr.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class OcrBoundingBox(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
) {
    @get:JsonIgnore
    val width: Int get() = maxX - minX

    @get:JsonIgnore
    val height: Int get() = maxY - minY

    @get:JsonIgnore
    val area: Int get() = width * height

    @get:JsonIgnore
    val centerX: Int get() = (minX + maxX) / 2

    @get:JsonIgnore
    val centerY: Int get() = (minY + maxY) / 2

    companion object {
        fun from(coordinates: List<OcrTextBlock.Coordinate>): OcrBoundingBox {
            if (coordinates.isEmpty()) return OcrBoundingBox(0, 0, 0, 0)
            val xList = coordinates.map { it.x }
            val yList = coordinates.map { it.y }
            return OcrBoundingBox(
                minX = xList.minOrNull() ?: 0,
                maxX = xList.maxOrNull() ?: 0,
                minY = yList.minOrNull() ?: 0,
                maxY = yList.maxOrNull() ?: 0
            )
        }
    }
}
