package com.example.yakallim.ocr.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ocr")
data class OcrProperties(
    val type: String,
    val engine: Engine,
    val parser: Parser
) {
    data class Engine(
        val onnx: Onnx
    ) {
        data class Onnx(
            val detectionModelPath: String,
            val recognitionModelPath: String,
            val recognitionDictionaryPath: String,
            val detectionThreshold: Float
        )
    }

    data class Parser(
        val yOffset: Int,
        val yDeviationThreshold: Int,
        val columnSeparatorX: Int,
        val medicineMinX: Int,
        val medicineMaxX: Int
    )
}