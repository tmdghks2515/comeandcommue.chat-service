# 1단계: 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# GitHub Packages 자격증명
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# 캐시 최적화: wrapper/메타 먼저
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle* settings.gradle* gradle.properties* ./
RUN set -eux; sed -i 's/\r$//' gradlew; chmod +x gradlew

# (옵션) 의존성 프리패치
RUN ./gradlew --no-daemon --version

# 소스
COPY src src

# 빌드
RUN ./gradlew --no-daemon clean bootJar -x test

# 2단계: 런타임
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
