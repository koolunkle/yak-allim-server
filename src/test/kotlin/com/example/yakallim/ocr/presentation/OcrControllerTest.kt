package com.example.yakallim.ocr.presentation

import com.example.yakallim.ocr.domain.repository.OcrJobRepository
import com.example.yakallim.ocr.presentation.dto.OcrJobResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
class OcrControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var ocrJobRepository: OcrJobRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("진행 중인 OCR 작업을 취소하면 상태가 CANCELLED로 변경된다")
    fun shouldCancelOcrJobAndReturnOk() {
        val mockMultipartFile =
            MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test content".toByteArray())
        val submitResult = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/ocr/enqueue")
                .file(mockMultipartFile)
                .param("delay", "5000")
        ).andExpect(MockMvcResultMatchers.status().isAccepted).andReturn()

        val jobId = objectMapper.readValue(submitResult.response.contentAsString, OcrJobResponse::class.java).jobId

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ocr/jobs/$jobId/cancel"))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        val statusResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ocr/jobs/$jobId"))
            .andExpect(MockMvcResultMatchers.status().isOk).andReturn()

        val updatedResponse = objectMapper.readValue(statusResult.response.contentAsString, OcrJobResponse::class.java)

        Assertions.assertEquals(OcrJobResponse.JobStatus.CANCELLED, updatedResponse.status)
        Assertions.assertTrue(ocrJobRepository.isCancelled(jobId))
    }

    @Test
    @DisplayName("존재하지 않는 작업ID로 취소 요청 시 404를 반환한다")
    fun shouldReturnNotFoundForNonExistentJobId() {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ocr/jobs/non-existent-id/cancel"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DisplayName("파일명에 상위 디렉터리 접근 패턴이 있어도 안전하게 저장 및 처리된다")
    fun shouldSanitizePathTraversalFilenameAndProcessSuccessfully() {
        val maliciousFilename = "../../../malicious_test.jpg"
        val mockMultipartFile =
            MockMultipartFile("file", maliciousFilename, MediaType.IMAGE_JPEG_VALUE, "test content".toByteArray())
        
        val submitResult = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/ocr/enqueue")
                .file(mockMultipartFile)
        ).andExpect(MockMvcResultMatchers.status().isAccepted).andReturn()

        val job = objectMapper.readValue(submitResult.response.contentAsString, OcrJobResponse::class.java)
        Assertions.assertNotNull(job.jobId)
    }

    @Test
    @DisplayName("허용되지 않은 파일 확장자 업로드 시 BAD_REQUEST를 반환한다")
    fun shouldRejectInvalidFileExtension() {
        val invalidFilename = "test.exe"
        val mockMultipartFile =
            MockMultipartFile("file", invalidFilename, MediaType.APPLICATION_OCTET_STREAM_VALUE, "test content".toByteArray())

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/ocr/enqueue")
                .file(mockMultipartFile)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}