FROM eclipse-temurin:17-jre

WORKDIR /app

# 타임존 설정 (Asia/Seoul)
ENV TZ=Asia/Seoul

# 빌드된 JAR 파일 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 기본 포트 노출 및 실행
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
