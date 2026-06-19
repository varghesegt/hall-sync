# --- Stage 1: Build Frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
# Cache dependencies
COPY allocate-artisan/package*.json ./
RUN npm install
# Build
COPY allocate-artisan/ ./
RUN npm run build

# --- Stage 2: Build Backend ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app/backend
COPY pom.xml ./
# Pre-fetch dependencies
RUN mvn dependency:go-offline

COPY src ./src
# Embed frontend into backend
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/

RUN mvn clean package -DskipTests

# --- Stage 3: Runtime (Production Grade) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install runtime dependencies: 
# - curl: for healthchecks
# - tini: for proper signal handling (prevents zombie processes)
RUN apk add --no-cache curl tini

# Create a non-root system user for security
RUN addgroup -S hallsync && adduser -S hallsync -G hallsync

# Setup directories with correct permissions for non-root user
RUN mkdir -p uploads logs data/parser-tmp && \
    chown -R hallsync:hallsync /app

# Switch to non-root user
USER hallsync

# Copy artifacts from build stages
COPY --from=backend-build --chown=hallsync:hallsync /app/backend/target/*.jar app.jar

# Standard Port
EXPOSE 8081

# Advanced Healthcheck: Verify Spring Boot Actuator is responding
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Use Tini as init to correctly pass signals to the JVM
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

