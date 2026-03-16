# ===========================================
# Stage 1: Build Backend (Java)
# ===========================================
FROM eclipse-temurin:17-jdk-alpine AS backend-builder

WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY specpulse-backend/build.gradle specpulse-backend/build.gradle

# Create empty directories to satisfy Gradle
RUN mkdir -p specpulse-frontend
RUN mkdir -p specpulse-backend/src/main/resources/static

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer for faster rebuilds)
RUN ./gradlew :specpulse-backend:dependencies --no-daemon || true

# Copy backend source code
COPY specpulse-backend/src specpulse-backend/src

# ===========================================
# Stage 2: Build Frontend (Node.js)
# ===========================================
FROM node:20-alpine AS frontend-builder

WORKDIR /app

# Copy package files
COPY specpulse-frontend/package.json specpulse-frontend/package-lock.json* ./

# Install dependencies
RUN npm ci

# Copy frontend source
COPY specpulse-frontend/ ./

# Build frontend
RUN npm run build

# ===========================================
# Stage 3: Final Backend Build with Static
# ===========================================
FROM eclipse-temurin:17-jdk-alpine AS backend-final

WORKDIR /app

# Copy everything from backend-builder
COPY --from=backend-builder /app .

# Copy frontend build to static resources
COPY --from=frontend-builder /app/dist specpulse-backend/src/main/resources/static

# Rebuild JAR with static files
RUN ./gradlew :specpulse-backend:bootJar --no-daemon

# ===========================================
# Stage 4: Runtime (JRE only)
# ===========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Copy backend JAR from final builder
COPY --from=backend-final /app/specpulse-backend/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
