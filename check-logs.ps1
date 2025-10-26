# Check logs for login attempts

Write-Host "Checking backend logs for login attempts..." -ForegroundColor Yellow

# Get last 200 lines and filter for relevant info
$logs = docker logs ucacue_app --tail 200 2>&1

Write-Host "`nSearching for 'Login attempt'..." -ForegroundColor Cyan
$logs | Select-String -Pattern "Login attempt" -Context 0,3

Write-Host "`nSearching for 'User found'..." -ForegroundColor Cyan
$logs | Select-String -Pattern "User found" -Context 0,2

Write-Host "`nSearching for 'Password matches'..." -ForegroundColor Cyan
$logs | Select-String -Pattern "Password matches" -Context 0,1

Write-Host "`nSearching for 'AuthService'..." -ForegroundColor Cyan
$logs | Select-String -Pattern "AuthService" -Context 0,2

Write-Host "`nSearching for any INFO logs..." -ForegroundColor Cyan
$logs | Select-String -Pattern "INFO" | Select-Object -Last 20
