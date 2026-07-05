package com.example.yakallim.ocr.domain.model

import kotlin.math.max
import kotlin.math.min

data class BoundingBox(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
) {
    val centerX: Int get() = (minX + maxX) / 2
    val centerY: Int get() = (minY + maxY) / 2
    val width: Int get() = maxX - minX
    val height: Int get() = maxY - minY
    val area: Int get() = width * height

    companion object {
        fun from(coordinates: List<TextBlock.Coordinate>): BoundingBox {
            if (coordinates.isEmpty()) return BoundingBox(0, 0, 0, 0)
            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE

            for (coordinate in coordinates) {
                minX = min(minX, coordinate.x)
                maxX = max(maxX, coordinate.x)
                minY = min(minY, coordinate.y)
                maxY = max(maxY, coordinate.y)
            }

            return BoundingBox(minX, maxX, minY, maxY)
        }
    }
}
