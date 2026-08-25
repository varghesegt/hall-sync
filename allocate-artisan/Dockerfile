# --- Stage 1: Build Frontend Assets ---
FROM node:20-alpine AS build
WORKDIR /app

# Cache package dependencies layer
COPY package*.json ./
RUN npm ci --quiet

# Copy source files and build production bundle
COPY . .
RUN npm run build

# --- Stage 2: Production Nginx Runtime ---
FROM nginx:1.25-alpine AS production

# Install curl for container health check
RUN apk add --no-cache curl

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy compiled SPA dist from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom production nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Production Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
