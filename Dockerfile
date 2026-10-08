# ==============================================================================
#  HallSync Enterprise Production Dockerfile
#  Multi-Stage Optimized Build for Spring Boot Backend & React Frontend
# ==============================================================================

# --- Stage 1: Build React Frontend ---
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY allocate-artisan/package*.json ./
RUN npm ci --quiet
COPY allocate-artisan/ ./
RUN npm run build

# --- Stage 2: Build Maven Backend ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app/backend

# Cache Maven dependencies layer
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy backend source code
COPY src ./src

# Clean stale static resources and copy fresh compiled SPA frontend dist into Spring Boot static resources
RUN rm -rf ./src/main/resources/static/*
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static/

# Package production executable JAR without running tests
RUN mvn clean package -DskipTests -B

# --- Stage 3: Minimal Production Runtime ---
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Install security patches & lightweight tools (tini for PID 1 signal forwarding, curl for healthcheck)
RUN apk add --no-cache curl tini tzdata && \
    cp /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && \
    echo "Asia/Kolkata" > /etc/timezone

# Security Hardening: Non-Root Execution Group and User
RUN addgroup -S -g 1001 hallsync && \
    adduser -S -u 1001 -G hallsync hallsync

# Create application data storage directories with correct ownership
RUN mkdir -p /app/uploads /app/logs /app/data/parser-tmp && \
    chown -R hallsync:hallsync /app

USER hallsync:hallsync

# Copy compiled Spring Boot executable JAR
COPY --from=backend-builder --chown=hallsync:hallsync /app/backend/target/hall-sync-*.jar /app/app.jar

# Server port (Render default)
EXPOSE 10000

# Production Container Health Check
HEALTHCHECK --interval=15s --timeout=5s --start-period=35s --retries=5 \
  CMD curl -f http://localhost:${PORT:-10000}/actuator/health || curl -f http://localhost:${PORT:-10000}/ || exit 1

# Production JVM Performance & Memory Tuning Flags (defaults to 512MB cloud free-tier safe footprint)
ENV JAVA_OPTS="-Xms128m -Xmx280m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -Xss512k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Kolkata"

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
