# Fix users with correct BCrypt hash

Write-Host "Fixing user passwords..." -ForegroundColor Yellow

# BCrypt hash for "Admin123!" - valid 60 character hash
$correctHash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'

Write-Host "Updating users with correct password hash..." -ForegroundColor Cyan

$sql = @"
USE ucacue_erp;
UPDATE users SET password_hash = '$correctHash' WHERE email IN ('admin@ucacue.edu.ec', 'comprador@ucacue.edu.ec', 'juan.perez@ucacue.edu.ec');
SELECT id, email, role, LENGTH(password_hash) as hash_length FROM users;
"@

$sql | docker exec -i ucacue_mysql mysql -uroot -proot

Write-Host "Users updated!" -ForegroundColor Green
Write-Host "Testing login..." -ForegroundColor Yellow

Start-Sleep -Seconds 2
powershell -ExecutionPolicy Bypass -File test-login.ps1
