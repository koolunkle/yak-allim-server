package com.example.yakallim.ocr.engine

import com.example.yakallim.ocr.model.TextBlock
import java.io.InputStream

interface OcrEngine {
    fun runOcr(imageStream: InputStream, jobId: String? = null): List<TextBlock>
}