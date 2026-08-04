# syntax=docker/dockerfile:1.7

# ===========================================
# Stage 1: Backend dependency warmup
# (Gradle build files only; cached until Gradle config changes)
# ===========================================
FROM eclipse-temurin:17-jdk-alpine AS backend-deps

WORKDIR /app

# Gradle wrapper and build scripts required for dependency resolution
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle
COPY specpulse-backend/build.gradle specpulse-backend/build.gradle

# Some Gradle configuration references frontend/static paths.
RUN mkdir -p specpulse-frontend \
    && mkdir -p specpulse-backend/src/main/resources/static

RUN chmod +x gradlew

# Download Gradle dependencies.
# BuildKit cache mount keeps Gradle User Home between builds.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :specpulse-backend:dependencies --no-daemon

# ===========================================
# Stage 2: Frontend dependencies (npm ci)
# (Reinstalled only when package.json / package-lock.json change)
# ===========================================
FROM node:20-alpine AS frontend-deps

WORKDIR /app

ENV npm_config_cache=/root/.npm

# Copy only files needed for npm dependency installation
COPY specpulse-frontend/package.json specpulse-frontend/package-lock.json* ./

RUN --mount=type=cache,target=/root/.npm \
    npm ci

# ===========================================
# Stage 3: Frontend build
# (Source changes invalidate this stage only)
# ===========================================
FROM node:20-alpine AS frontend-build

WORKDIR /app

# Reuse node_modules from dependency stage
COPY --from=frontend-deps /app/node_modules ./node_modules

# Copy frontend sources
COPY specpulse-frontend/ ./

RUN npm run build

# ===========================================
# Stage 4: Backend build (bootJar + embedded frontend)
# ===========================================
FROM eclipse-temurin:17-jdk-alpine AS backend-build

WORKDIR /app

# Bring in Gradle build scripts and wrapper
COPY --from=backend-deps /app .

# Copy backend sources only after dependency warmup
COPY specpulse-backend/src specpulse-backend/src

# Copy built frontend to backend static resources
COPY --from=frontend-build /app/dist specpulse-backend/src/main/resources/static

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :specpulse-backend:bootJar --no-daemon

# Normalize JAR output for the runtime stage
RUN set -eu; \
    BOOT_JAR=""; \
    BOOT_JAR_COUNT=0; \
    for candidate in specpulse-backend/build/libs/*.jar; do \
      case "$candidate" in \
        *-plain.jar) ;; \
        *) BOOT_JAR="$candidate"; BOOT_JAR_COUNT=$((BOOT_JAR_COUNT + 1)) ;; \
      esac; \
    done; \
    test "$BOOT_JAR_COUNT" -eq 1; \
    cp "$BOOT_JAR" /app/app.jar

# ===========================================
# Stage 5: Runtime (JRE only, non-root)
# ===========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 appgroup \
    && adduser -u 1001 -G appgroup -D appuser

COPY --from=backend-build /app/app.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms1g -Xmx1g -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
