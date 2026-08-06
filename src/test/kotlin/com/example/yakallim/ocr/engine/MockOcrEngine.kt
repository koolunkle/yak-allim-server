package com.example.yakallim.ocr.engine

import com.example.yakallim.ocr.model.OcrTextBlock
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
@Profile("test")
class MockOcrEngine : OcrEngine {
    override fun runOcr(imageStream: InputStream, jobId: String?): List<OcrTextBlock> {
        return listOf(
            OcrTextBlock("약품명", 1.0f, createBounds(80, 340, 20, 60)),
            OcrTextBlock("복약안내(투약량/횟수/일수)", 1.0f, createBounds(340, 600, 20, 60)),
            OcrTextBlock("타이레놀정 500mg", 1.0f, createBounds(80, 260, 80, 120)),
            OcrTextBlock("1정 / 3회 / 3일", 1.0f, createBounds(340, 500, 80, 120)),
            OcrTextBlock("아모디핀정", 1.0f, createBounds(80, 200, 140, 180)),
            OcrTextBlock("1.5정 / 2회 / 7일", 1.0f, createBounds(340, 500, 140, 180))
        )
    }

    private fun createBounds(minX: Int, maxX: Int, minY: Int, maxY: Int): List<OcrTextBlock.Coordinate> {
        return listOf(
            OcrTextBlock.Coordinate(minX, minY),
            OcrTextBlock.Coordinate(maxX, minY),
            OcrTextBlock.Coordinate(maxX, maxY),
            OcrTextBlock.Coordinate(minX, maxY)
        )
    }
}