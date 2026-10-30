# ===================================================================
# Stage 1: Build JAR using Gradle Wrapper & Eclipse Temurin JDK 17
# ===================================================================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy Gradle wrapper and configuration files first (for Docker layer caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# Copy application source code and build executable JAR (optimized for 512MB RAM cloud tiers)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -Dorg.gradle.jvmargs="-Xmx384m -XX:MaxMetaspaceSize=192m"

# ===================================================================
# Stage 2: Lightweight Production Runtime (Eclipse Temurin JRE 17)
# ===================================================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Default environment variables for Production / Cloud (Render, Railway, etc.)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

# Run with memory tuning suited for 512MB container environments
ENTRYPOINT ["sh", "-c", "java -Xmx350m -Xss512k -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
