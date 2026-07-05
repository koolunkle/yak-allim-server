package com.example.yakallim.global.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HangulUtilsTest {

    @Test
    @DisplayName("한글 문자열을 초성, 중성, 종성 자모로 분리 및 표준화한다")
    fun shouldNormalizeHangulToJamo() {
        assertEquals("ㅇㅏㄴㄴㅕㅇ", HangulUtils.normalizeToJamo("안녕"))
        assertEquals("ㅎㅏㄴㄱㅡㄹ", HangulUtils.normalizeToJamo("한글"))
        assertEquals("ㄱㅏㄱㅏㄱㅏ", HangulUtils.normalizeToJamo("가가가"))
    }

    @Test
    @DisplayName("받침(종성)이 없는 한글 문자도 자모를 분리한다")
    fun shouldHandleCharactersWithoutFinalConsonant() {
        assertEquals("ㄱㅏ", HangulUtils.normalizeToJamo("가"))
        assertEquals("ㄴㅏ", HangulUtils.normalizeToJamo("나"))
    }

    @Test
    @DisplayName("한글이 아닌 숫자, 영문, 특수문자는 분리하지 않고 유지한다")
    fun shouldPreserveNonHangulCharacters() {
        assertEquals("ㅇㅏㄹㅇㅑㄱ123!", HangulUtils.normalizeToJamo("알약123!"))
        assertEquals("Tablet", HangulUtils.normalizeToJamo("Tablet"))
    }

    @Test
    @DisplayName("두 자모 분리 문자열 간의 편집 거리(Levenshtein Distance)를 계산한다")
    fun shouldCalculateLevenshteinDistance() {
        assertEquals(0, HangulUtils.levenshteinDistanceTo("ㅇㅏㄴㄴㅕㅇ", "ㅇㅏㄴㄴㅕㅇ"))
        assertEquals(2, HangulUtils.levenshteinDistanceTo("ㅇㅏㄴㄴㅕㅇ", "ㅇㅏㄴㄴㅕㅇㅎㅏ"))
        assertEquals(1, HangulUtils.levenshteinDistanceTo("ㄱㅏ", "ㄴㅏ"))
        assertEquals(3, HangulUtils.levenshteinDistanceTo("", "ㅇㅏㄴ"))
    }
}
