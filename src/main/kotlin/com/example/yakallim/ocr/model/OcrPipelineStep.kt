package com.example.yakallim.ocr.model

enum class OcrPipelineStep(val defaultMessage: String, val defaultProgress: Int) {
    ACCEPTED("작업이 대기열에 등록되었습니다.", 5),
    IMAGE_PROCESSING("이미지 전처리 중...", 15),
    TEXT_DETECTION("텍스트 영역 검출 중...", 35),
    TEXT_RECOGNITION("텍스트 인식 중...", 65),
    PARSING("처방 정보 분석 및 의약품 매칭 중...", 85),
    COMPLETED("분석이 완료되었습니다.", 100),
    FAILED("분석에 실패하였습니다.", 100)
}
