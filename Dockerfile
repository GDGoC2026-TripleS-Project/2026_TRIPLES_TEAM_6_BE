# 1) 빌드 단계 (builder stage)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle wrapper + 설정 파일 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle* settings.gradle* gradle.properties* ./

# 소스 복사
COPY src src

# 테스트는 일단 제외
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# 2) 실행 단계 (runtime stage)
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
