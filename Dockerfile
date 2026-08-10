# --- Stage 1: Build Frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY allocate-artisan/package*.json ./
RUN npm install
COPY allocate-artisan/ ./
RUN npm run build

# --- Stage 2: Build Backend ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app/backend
COPY pom.xml ./
# Cache dependencies layer
RUN mvn dependency:go-offline -B || true
COPY src ./src
# Embed compiled frontend build into backend static resources
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/
RUN mvn clean package -DskipTests

# --- Stage 3: Production Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install runtime dependencies (curl for healthcheck, tini for process signal management)
RUN apk add --no-cache curl tini

# Create non-root user for security
RUN addgroup -S hallsync && adduser -S hallsync -G hallsync

# Setup application directories with permissions
RUN mkdir -p uploads logs data/parser-tmp && \
    chown -R hallsync:hallsync /app

USER hallsync

# Copy compiled JAR
COPY --from=backend-build --chown=hallsync:hallsync /app/backend/target/*.jar app.jar

EXPOSE 8081

# Production Actuator Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
