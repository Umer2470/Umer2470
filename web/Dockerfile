# Production Dockerfile for Choudhury POS Web Application
FROM node:22-alpine

# Set working directory
WORKDIR /app

# Set production environment
ENV NODE_ENV=production
ENV APP_PORT=3000
ENV DB_PATH=/data/pos_central.db

# Create persistent data and downloads directories
RUN mkdir -p /data /app/public/downloads

# Copy application source code
COPY server.js ./
COPY public ./public

# Expose port 3000
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:3000/api/health || exit 1

# Start zero-dependency native Node server
CMD ["node", "server.js"]
