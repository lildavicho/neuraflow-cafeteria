FROM nginx:stable-alpine

# Copy static portal files
COPY . /usr/share/nginx/html/

# Remove Nginx default index.html to avoid conflicts
RUN rm -f /usr/share/nginx/html/index.html

# Copy nginx config
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:80/ || exit 1
