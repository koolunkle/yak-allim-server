package com.example.yakallim.ocr.domain.model

enum class PipelineStep(val defaultMessage: String, val defaultProgress: Int) {
    ENQUEUED("요청이 접수되었습니다. 대기 중입니다...", 5),
    IMAGE_PROCESSING("이미지 전처리 및 노이즈 제거 중입니다...", 25),
    TEXT_DETECTION("복약 안내서 내 텍스트 영역을 찾는 중입니다...", 50),
    TEXT_RECOGNITION("글자 판독 및 텍스트를 추출하는 중입니다...", 75),
    EXPORT_RESULT("결과 데이터를 변환하는 중입니다...", 95),
    COMPLETED("분석이 완료되었습니다.", 100),
    FAILED("분석에 실패하였습니다.", 100)
}
