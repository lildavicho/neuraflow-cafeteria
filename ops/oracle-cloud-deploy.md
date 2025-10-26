# Oracle Cloud Deployment Guide for UCACUE Bar System

## Prerequisites

1. Oracle Cloud Account with credits
2. Domain name (optional, can use Oracle subdomain)
3. SSH key pair for VM access

## Step 1: Create Compute Instance

1. Navigate to **Compute → Instances** in OCI Console
2. Click **Create Instance**
3. Configure:
   - **Name**: `ucacue-bar-prod`
   - **Image**: Ubuntu 22.04 LTS
   - **Shape**: VM.Standard.E2.1.Micro (Always Free) or VM.Standard.A1.Flex (ARM)
   - **VCN**: Create new or use existing
   - **Subnet**: Public subnet
   - **SSH Keys**: Upload your public key
4. Click **Create**

## Step 2: Configure Security Lists

1. Go to **Networking → Virtual Cloud Networks**
2. Select your VCN → Security Lists
3. Add Ingress Rules:
   ```
   - HTTP: Source 0.0.0.0/0, Port 80
   - HTTPS: Source 0.0.0.0/0, Port 443
   - SSH: Source YOUR_IP/32, Port 22
   - App: Source 0.0.0.0/0, Port 8080
   ```

## Step 3: Connect to Instance

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@<PUBLIC_IP>
```

## Step 4: Install Docker and Dependencies

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Git
sudo apt install -y git

# Install Nginx
sudo apt install -y nginx certbot python3-certbot-nginx

# Logout and login to apply docker group
exit
ssh -i ~/.ssh/your-key.pem ubuntu@<PUBLIC_IP>
```

## Step 5: Clone and Configure Application

```bash
# Clone repository
git clone https://github.com/ucacue/bar-spring.git
cd ucacue-bar-spring

# Create production environment file
cp .env.example .env.prod
nano .env.prod
```

Update `.env.prod` with production values:
```env
# Database
DB_URL=mysql-prod:3306
DB_USER=ucacue_prod
DB_PASS=<STRONG_PASSWORD>

# JWT (generate new secret)
JWT_SECRET=<64_CHAR_RANDOM_STRING>

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=notifications@ucacue.edu.ec
SMTP_PASS=<APP_PASSWORD>

# Firebase
FIREBASE_PROJECT_ID=ucacue-bar-prod
FIREBASE_API_KEY=<YOUR_API_KEY>

# Algolia
ALGOLIA_APP_ID=<YOUR_APP_ID>
ALGOLIA_API_KEY=<YOUR_ADMIN_KEY>

# Payment Gateways
PAYPHONE_KEY=<PRODUCTION_KEY>
DATAFAST_KEY=<PRODUCTION_KEY>

# Application
SERVER_PORT=8080
CONTEXT_PATH=/api
DDL_AUTO=validate
SHOW_SQL=false
LOG_LEVEL=INFO
```

## Step 6: Setup MySQL Database

### Option A: Oracle MySQL Database Service (Recommended)
1. Create MySQL DB System in OCI Console
2. Configure private endpoint
3. Update `.env.prod` with connection details

### Option B: Docker MySQL
```bash
# Create data directory
mkdir -p ~/mysql-data

# Run MySQL container
docker run -d \
  --name mysql-prod \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD=<ROOT_PASSWORD> \
  -e MYSQL_DATABASE=ucacue_bar \
  -e MYSQL_USER=ucacue_prod \
  -e MYSQL_PASSWORD=<DB_PASSWORD> \
  -v ~/mysql-data:/var/lib/mysql \
  -p 3306:3306 \
  mysql:8.0

# Import schema
docker exec -i mysql-prod mysql -uucacue_prod -p<DB_PASSWORD> ucacue_bar < db/database_setup.sql
```

## Step 7: Build and Deploy Application

```bash
# Create production docker-compose file
cat > docker-compose.prod.yml << 'EOF'
version: '3.8'

services:
  app:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: ucacue_app
    restart: unless-stopped
    env_file: .env.prod
    ports:
      - "8080:8080"
    networks:
      - ucacue_net
    volumes:
      - ./logs:/app/logs
      - ./uploads:/app/uploads
    depends_on:
      - redis

  redis:
    image: redis:7-alpine
    container_name: ucacue_redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    networks:
      - ucacue_net

  mediamtx:
    image: bluenviron/mediamtx:latest
    container_name: ucacue_mediamtx
    restart: unless-stopped
    ports:
      - "8554:8554"
      - "1935:1935"
      - "8889:8889"
    volumes:
      - ./ops/mediamtx.yml:/mediamtx.yml
    networks:
      - ucacue_net

networks:
  ucacue_net:
    driver: bridge
EOF

# Build and start services
docker-compose -f docker-compose.prod.yml up -d --build

# Check logs
docker-compose -f docker-compose.prod.yml logs -f app
```

## Step 8: Configure Nginx

```bash
# Create Nginx configuration
sudo nano /etc/nginx/sites-available/ucacue-bar
```

Add configuration:
```nginx
server {
    listen 80;
    server_name bar.ucacue.edu.ec;
    
    # Frontend
    location / {
        root /home/ubuntu/ucacue-bar-spring/frontend;
        try_files $uri $uri/ /pages/login.html;
    }
    
    # API Proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
    
    # WebRTC
    location /webrtc/ {
        proxy_pass http://localhost:8889/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # SSE endpoint
    location /api/dashboard/metrics/sse {
        proxy_pass http://localhost:8080/api/dashboard/metrics/sse;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
    }
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/ucacue-bar /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Step 9: Setup HTTPS with Let's Encrypt

```bash
# Install certificate
sudo certbot --nginx -d bar.ucacue.edu.ec

# Auto-renewal
sudo certbot renew --dry-run
```

## Step 10: Setup Monitoring

```bash
# Install monitoring stack
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v ~/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

## Step 11: Backup Strategy

Create backup script:
```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/home/ubuntu/backups"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
docker exec mysql-prod mysqldump -uroot -p<ROOT_PASSWORD> ucacue_bar | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# Backup uploads
tar czf $BACKUP_DIR/uploads_$DATE.tar.gz /home/ubuntu/ucacue-bar-spring/uploads

# Keep only last 7 days of backups
find $BACKUP_DIR -type f -mtime +7 -delete

# Upload to Object Storage (optional)
# oci os object put --bucket-name backups --file $BACKUP_DIR/db_$DATE.sql.gz
```

Setup cron job:
```bash
crontab -e
# Add: 0 2 * * * /home/ubuntu/backup.sh
```

## Step 12: Performance Tuning

### Application Memory
```bash
# Edit docker-compose.prod.yml
services:
  app:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1g -XX:+UseG1GC"
```

### MySQL Tuning
```sql
-- Add to MySQL configuration
SET GLOBAL max_connections = 200;
SET GLOBAL innodb_buffer_pool_size = 512M;
```

### Nginx Caching
```nginx
# Add to nginx config
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

## Step 13: Security Hardening

```bash
# Setup firewall
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw enable

# Fail2ban for SSH protection
sudo apt install fail2ban
sudo systemctl enable fail2ban

# Regular updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure unattended-upgrades
```

## Step 14: Monitoring Commands

```bash
# Check application status
docker ps
docker logs ucacue_app --tail 100

# Check resource usage
docker stats
htop

# Check disk space
df -h

# Check application health
curl http://localhost:8080/api/actuator/health
```

## Troubleshooting

### Application won't start
```bash
docker logs ucacue_app
# Check for configuration issues
```

### Database connection issues
```bash
docker exec -it mysql-prod mysql -uroot -p
# Test connection manually
```

### High memory usage
```bash
docker restart ucacue_app
# Consider upgrading instance
```

## Production Checklist

- [ ] Domain configured and pointing to server
- [ ] HTTPS enabled with valid certificate
- [ ] Environment variables secured
- [ ] Database backups configured
- [ ] Monitoring setup
- [ ] Log rotation configured
- [ ] Firewall rules applied
- [ ] SSH key-only access
- [ ] Regular updates scheduled
- [ ] Error tracking configured
- [ ] Performance monitoring active
- [ ] Disaster recovery plan documented

## Support

For issues or questions:
- Email: devops@ucacue.edu.ec
- Documentation: https://docs.ucacue.edu.ec/bar-system
- Emergency: +593 7 2831608

---
Last Updated: October 2025
