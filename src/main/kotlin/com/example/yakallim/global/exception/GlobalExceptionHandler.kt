package com.example.yakallim.global.exception

import com.example.yakallim.ocr.domain.exception.OcrException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(OcrException::class)
    fun handleOcrException(ex: OcrException): ResponseEntity<ErrorResponse> {
        log.error("OCR API 예외 발생: [{}], 메시지: {}", ex.status, ex.message)
        val response = ErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message
        )
        return ResponseEntity.status(ex.status).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("처리되지 않은 서버 내부 예외 발생", ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val response = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "서버 내부 오류가 발생했습니다."
        )
        return ResponseEntity.status(status).body(response)
    }
}
