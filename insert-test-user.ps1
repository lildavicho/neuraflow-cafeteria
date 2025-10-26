# Insert test user with known working BCrypt hash

Write-Host "Inserting test user..." -ForegroundColor Yellow

# This is a BCrypt hash for "password123" that I know works
# Generated from: https://bcrypt-generator.com/
$testHash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRJzQQQ4e'

$sql = @"
USE ucacue_erp;

-- Delete existing test user
DELETE FROM users WHERE email = 'test@ucacue.edu.ec';

-- Insert test user with password: password123
INSERT INTO users (full_name, email, password_hash, identification, phone, role, active) 
VALUES ('Test User', 'test@ucacue.edu.ec', '$testHash', '0104567890', '0999999999', 'ADMIN', true);

-- Show all users
SELECT id, email, role, LENGTH(password_hash) as hash_len FROM users;
"@

$sql | docker exec -i ucacue_mysql mysql -uroot -proot

Write-Host "`nTest user created!" -ForegroundColor Green
Write-Host "Email: test@ucacue.edu.ec" -ForegroundColor Cyan
Write-Host "Password: password123" -ForegroundColor Cyan

Write-Host "`nTesting login with test user..." -ForegroundColor Yellow

$body = @{
    email = "test@ucacue.edu.ec"
    password = "password123"
    require2FA = $false
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    Write-Host "`nSUCCESS! Login worked!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "`nFailed!" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Yellow
    }
}
