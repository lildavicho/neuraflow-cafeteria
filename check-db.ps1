# Check database users

Write-Host "Checking MySQL database..." -ForegroundColor Yellow

$query = "SELECT id, email, full_name, role, created_at FROM users;"

try {
    $result = docker exec ucacue_mysql mysql -uucacue_user -pucacue_pass ucacue_erp -e $query 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ Database query successful!" -ForegroundColor Green
        Write-Host "`nUsers in database:" -ForegroundColor Cyan
        Write-Host $result
    } else {
        Write-Host "`n❌ Database query failed!" -ForegroundColor Red
    }
} catch {
    Write-Host "`n❌ Error: $_" -ForegroundColor Red
}

# Also check if database exists
Write-Host "`nChecking if database exists..." -ForegroundColor Yellow
$dbCheck = docker exec ucacue_mysql mysql -uucacue_user -pucacue_pass -e "SHOW DATABASES;" 2>$null
Write-Host $dbCheck
