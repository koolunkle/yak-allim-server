# TASK-01: Firebase 및 ONNX 의존성 모킹을 통한 로컬 및 CI 테스트 구축

## 1. 목적 (Context)
- 현재 프로젝트는 로컬에 `yak-allim-firebase-key.json`이나 `*.onnx` 파일이 존재해야만 Spring Boot context가 로드되어 빌드 및 테스트가 통과되는 강한 결합을 가지고 있습니다.
- 이를 해결하기 위해 테스트 환경(`test` 프로파일)에서는 실제 외부 자산을 사용하지 않고, 모의 객체(Mock) 또는 하드코딩된 결과를 반환하는 구현체를 주입하도록 리팩토링합니다.
- 최종적으로 CI 환경(`.github/workflows/ci.yml`)에서도 `-x test` 없이 `./gradlew test`를 정상적으로 통과시키는 것을 목표로 합니다.

## 2. 세부 구현 요구사항 (Todo)
- [x] Firebase 초기화 및 관련 빈(Bean)을 인터페이스화하거나, `test` 프로파일일 때는 모의 빈(Mock)이 등록되도록 스프링 설정 수정
- [x] ONNX 모델 파일이 로컬 클래스패스에 없어도 `OcrEngine` 관련 테스트나 애플리케이션 초기화가 실패하지 않도록 Mock 또는 가짜 엔진 빈 생성
- [x] `YakallimApplicationTests`를 실행했을 때 두 외부 의존성 예외 없이 통과되는지 확인


## 3. 하네스 제약 조건 (Harness)
- 수정/생성해야 하는 파일: `OcrEngine.kt`, `application.properties`, 관련 설정 파일 등
- 검증 명령어: `./gradlew test`
- 규칙: `./gradlew test`가 어떠한 외부 파일 부재 에러 없이 완전히 성공(Green)할 때까지 작업을 마쳐야 합니다.
