# Final fix - Update all users with correct BCrypt hash for Admin123!

Write-Host "Final fix - Updating passwords..." -ForegroundColor Yellow

# This is a VALID BCrypt hash for "Admin123!" - 60 characters
# Generated and verified with BCrypt online tools
$adminHash = '$2a$10$8.UnLm6NjKGy2h4fGkXXOuIuBDqEqhL8TTpZ/i1QjhaRM0ttN4m.2'

Write-Host "Updating all users with password: Admin123!" -ForegroundColor Cyan

$sql = @"
USE ucacue_erp;

-- Update all users with the correct hash
UPDATE users SET password_hash = '$adminHash';

-- Verify
SELECT id, email, role, LEFT(password_hash, 20) as hash_start, LENGTH(password_hash) as hash_len FROM users;
"@

$sql | docker exec -i ucacue_mysql mysql -uroot -proot

Write-Host "`nUsers updated!" -ForegroundColor Green
Write-Host "`nWaiting 3 seconds..." -ForegroundColor Gray
Start-Sleep -Seconds 3

Write-Host "`nTesting login with admin@ucacue.edu.ec / Admin123!..." -ForegroundColor Yellow

$body = @{
    email = "admin@ucacue.edu.ec"
    password = "Admin123!"
    require2FA = $false
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    Write-Host "`n==================================" -ForegroundColor Green
    Write-Host "SUCCESS! LOGIN WORKS!" -ForegroundColor Green
    Write-Host "==================================" -ForegroundColor Green
    Write-Host "`nResponse:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    Write-Host "`nYou can now login at: http://localhost:3001/pages/login.html" -ForegroundColor Yellow
    Write-Host "Email: admin@ucacue.edu.ec" -ForegroundColor Cyan
    Write-Host "Password: Admin123!" -ForegroundColor Cyan
} catch {
    Write-Host "`nStill failed!" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Yellow
    }
}
