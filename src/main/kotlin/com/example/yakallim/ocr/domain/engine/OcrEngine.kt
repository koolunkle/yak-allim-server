package com.example.yakallim.ocr.domain.engine

import com.example.yakallim.ocr.domain.model.TextBlock
import java.io.InputStream

interface OcrEngine {
    fun runOcr(imageStream: InputStream, jobId: String? = null): List<TextBlock>
}