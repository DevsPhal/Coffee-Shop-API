# ==========================
# Stage 1 - Build
# ==========================
FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && gradle dependencies --no-daemon

COPY src/main/resources ./src/main/resources
COPY src/main/java ./src/main/java
COPY src/test ./src/test

RUN gradle clean bootJar -x test --no-daemon --build-cache

# ==========================
# Stage 2 - Runtime
# ==========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]