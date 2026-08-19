FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY app/build.gradle.kts app/build.gradle.kts
COPY auth-api auth-api

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew :auth-api:installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app \
    && adduser -S -G app app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/auth-api/build/install/auth-api/ ./

USER app
EXPOSE 8080

ENTRYPOINT ["/app/bin/auth-api"]
