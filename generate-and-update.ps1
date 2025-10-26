# Generate hash and update database

Write-Host "Generating BCrypt hash for Admin123!..." -ForegroundColor Yellow

Start-Sleep -Seconds 20

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/generate-hash?password=Admin123!" -Method Get
    
    Write-Host "`nHash generated successfully!" -ForegroundColor Green
    Write-Host "Password: $($response.password)" -ForegroundColor Cyan
    Write-Host "Hash: $($response.hash)" -ForegroundColor Cyan
    Write-Host "Length: $($response.length)" -ForegroundColor Cyan
    
    $hash = $response.hash
    
    Write-Host "`nUpdating database..." -ForegroundColor Yellow
    
    $sql = @"
USE ucacue_erp;
UPDATE users SET password_hash = '$hash';
SELECT id, email, role, LEFT(password_hash, 30) as hash_start, LENGTH(password_hash) as len FROM users;
"@
    
    $sql | docker exec -i ucacue_mysql mysql -uroot -proot
    
    Write-Host "`nDatabase updated!" -ForegroundColor Green
    Write-Host "`nTesting login..." -ForegroundColor Yellow
    
    Start-Sleep -Seconds 2
    
    $body = @{
        email = "admin@ucacue.edu.ec"
        password = "Admin123!"
        require2FA = $false
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "SUCCESS! LOGIN WORKS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "`nToken: $($loginResponse.token.Substring(0, 50))..." -ForegroundColor Cyan
    Write-Host "User: $($loginResponse.email)" -ForegroundColor Cyan
    Write-Host "Role: $($loginResponse.role)" -ForegroundColor Cyan
    Write-Host "`nYou can now login at: http://localhost:3001/pages/login.html" -ForegroundColor Yellow
    Write-Host "Email: admin@ucacue.edu.ec" -ForegroundColor Cyan
    Write-Host "Password: Admin123!" -ForegroundColor Cyan
    
} catch {
    Write-Host "`nError occurred!" -ForegroundColor Red
    Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Yellow
    }
}
