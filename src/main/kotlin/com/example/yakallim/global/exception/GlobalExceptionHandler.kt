package com.example.yakallim.global.exception

import com.example.yakallim.ocr.exception.OcrException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(OcrException::class)
    fun handleOcrException(ex: OcrException): ResponseEntity<ErrorResponse> {
        log.error("OCR API exception occurred: [{}], message: {}", ex.status, ex.message)
        val response = ErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message
        )
        return ResponseEntity.status(ex.status).contentType(MediaType.APPLICATION_JSON).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled internal server exception occurred", ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val response = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "서버 내부 오류가 발생했습니다."
        )
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response)
    }

    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleAsyncRequestNotUsableException() {
        log.warn("SSE stream unavailable due to client disconnect")
    }
}
