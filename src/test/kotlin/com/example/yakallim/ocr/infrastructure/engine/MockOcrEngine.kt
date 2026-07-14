package com.example.yakallim.ocr.infrastructure.engine

import com.example.yakallim.ocr.domain.engine.OcrEngine
import com.example.yakallim.ocr.domain.model.TextBlock
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
@Profile("test")
class MockOcrEngine : OcrEngine {
    /**
     * Returns fixed OCR results for test scenarios.
     *
     * @param imageStream Ignored.
     * @param jobId Ignored.
     * @return A predefined list of recognized text blocks.
     */
    override fun runOcr(imageStream: InputStream, jobId: String?): List<TextBlock> {
        return listOf(
            TextBlock("약품명", 1.0f, createBounds(80, 340, 20, 60)),
            TextBlock("복약안내(투약량/횟수/일수)", 1.0f, createBounds(340, 600, 20, 60)),
            TextBlock("타이레놀정 500mg", 1.0f, createBounds(80, 260, 80, 120)),
            TextBlock("1정 / 3회 / 3일", 1.0f, createBounds(340, 500, 80, 120)),
            TextBlock("아모디핀정", 1.0f, createBounds(80, 200, 140, 180)),
            TextBlock("1.5정 / 2회 / 7일", 1.0f, createBounds(340, 500, 140, 180))
        )
    }

    /**
     * Builds rectangular bounds from the given corner coordinates.
     *
     * @param minX The x-coordinate of the left edge.
     * @param maxX The x-coordinate of the right edge.
     * @param minY The y-coordinate of the top edge.
     * @param maxY The y-coordinate of the bottom edge.
     * @return A list of four coordinates representing the rectangle's corners.
     */
    private fun createBounds(minX: Int, maxX: Int, minY: Int, maxY: Int): List<TextBlock.Coordinate> {
        return listOf(
            TextBlock.Coordinate(minX, minY),
            TextBlock.Coordinate(maxX, minY),
            TextBlock.Coordinate(maxX, maxY),
            TextBlock.Coordinate(minX, maxY)
        )
    }
}