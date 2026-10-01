# ==============================================================================
# CHOUDHURY POS — PRODUCTION DOCKER CONTAINER
# Compatible with Google Cloud Run, AWS ECS, DigitalOcean, Railway, Render, VPS
# ==============================================================================
FROM node:22-alpine

ENV NODE_ENV=production
ENV DOCKER_PRODUCTION=true
ENV PORT=8080
ENV DB_PATH=/app/data/pos_central.db

# Install sqlite and bash for backup/restore utilities and health checks
RUN apk add --no-cache sqlite bash

WORKDIR /app

# Install dependencies first for optimal Docker layer caching
COPY package*.json ./
RUN npm ci --only=production

# Copy application source, web assets, and administration scripts
COPY server.js ./
COPY public/ ./public/
COPY scripts/ ./scripts/
RUN chmod +x ./scripts/*.sh

# Create persistent database mount point
RUN mkdir -p /app/data

# Cloud Run / Web standard port
EXPOSE 8080

# Container Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/api/health || exit 1

CMD ["node", "server.js"]
