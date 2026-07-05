package com.example.yakallim.ocr.domain.engine

import com.example.yakallim.ocr.domain.model.TextBlock
import java.io.InputStream

fun interface OcrEngine {
    fun runOcr(imageStream: InputStream): List<TextBlock>
}