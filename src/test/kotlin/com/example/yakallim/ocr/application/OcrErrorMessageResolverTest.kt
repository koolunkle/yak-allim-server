package com.example.yakallim.ocr.application

import com.example.yakallim.ocr.domain.exception.OcrException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OcrErrorMessageResolverTest {

    @Test
    @DisplayName("InvalidFileExtensionException 발생 시 사용자 친화적인 메시지를 반환한다")
    fun shouldReturnUserFriendlyMessageForInvalidFileExtension() {
        val exception = OcrException.InvalidFileExtensionException("txt")
        val message = OcrErrorMessageResolver.resolve(exception)
        assertEquals("지원하지 않는 이미지 형식입니다. JPG 또는 PNG 이미지 파일로 다시 올려주세요.", message)
    }

    @Test
    @DisplayName("EmptyFileException 발생 시 사용자 친화적인 메시지를 반환한다")
    fun shouldReturnUserFriendlyMessageForEmptyFile() {
        val exception = OcrException.EmptyFileException()
        val message = OcrErrorMessageResolver.resolve(exception)
        assertEquals("업로드된 이미지 파일이 비어 있습니다. 이미지를 다시 확인해 주세요.", message)
    }

    @Test
    @DisplayName("일반 예외 발생 시 표준 안내 메시지를 반환한다")
    fun shouldReturnDefaultUserFriendlyMessageForGeneralException() {
        val exception = RuntimeException("502 Bad Gateway from n8n webhook")
        val message = OcrErrorMessageResolver.resolve(exception)
        assertEquals("복약 안내서 분석 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", message)
    }
}
