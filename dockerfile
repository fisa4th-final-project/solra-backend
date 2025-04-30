# 1. Build stage (optional if you use CI build)
# 이 단계는 GitHub Actions에서 이미 빌드한다면 생략 가능

# 2. Run stage
FROM eclipse-temurin:17-jdk-alpine

# 환경 변수 설정
ENV APP_HOME=/app
WORKDIR $APP_HOME

# JAR 파일 복사 (CI에서 만든 빌드 산출물 기준)
COPY app/build/libs/*.jar app.jar

# 포트 오픈 (선택)
EXPOSE 8080

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
