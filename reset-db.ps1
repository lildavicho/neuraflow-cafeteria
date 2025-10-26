# Reset database with correct data

Write-Host "🔄 Resetting database..." -ForegroundColor Yellow

# Stop containers
Write-Host "`n1. Stopping containers..." -ForegroundColor Cyan
docker compose -f ops/docker-compose.yml down -v

# Start only MySQL
Write-Host "`n2. Starting MySQL..." -ForegroundColor Cyan
docker compose -f ops/docker-compose.yml up -d mysql

# Wait for MySQL to be ready
Write-Host "`n3. Waiting for MySQL to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 15

# Run database setup
Write-Host "`n4. Running database setup..." -ForegroundColor Cyan
Get-Content db/database_setup.sql | docker exec -i ucacue_mysql mysql -uroot -proot

# Run seed data
Write-Host "`n5. Inserting seed data..." -ForegroundColor Cyan
Get-Content db/seed_dev.sql | docker exec -i ucacue_mysql mysql -uroot -proot

# Verify users
Write-Host "`n6. Verifying users..." -ForegroundColor Cyan
docker exec ucacue_mysql mysql -uucacue_user -pucacue_pass ucacue_erp -e "SELECT id, email, role, active FROM users;"

# Start all services
Write-Host "`n7. Starting all services..." -ForegroundColor Cyan
docker compose -f ops/docker-compose.yml up -d

Write-Host "`n✅ Database reset complete!" -ForegroundColor Green
Write-Host "`nWait 30 seconds for backend to start, then test login at:" -ForegroundColor Yellow
Write-Host "http://localhost:3001/pages/login.html" -ForegroundColor Cyan
