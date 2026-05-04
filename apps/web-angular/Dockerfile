FROM node:20-alpine AS builder
WORKDIR /app

# Force rebuild on code changes
ARG BUILD_TIMESTAMP=2026-05-03-001
RUN echo "Build: ${BUILD_TIMESTAMP}"

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist/frontend-angular/browser/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:80/ || exit 1
