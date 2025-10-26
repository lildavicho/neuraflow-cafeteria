# Build backend and download dependencies

Write-Host "Building backend and downloading dependencies..." -ForegroundColor Cyan

Set-Location backend

Write-Host "`nCleaning previous builds..." -ForegroundColor Yellow
mvn clean

Write-Host "`nDownloading dependencies and compiling..." -ForegroundColor Yellow
mvn compile

Write-Host "`nPackaging application..." -ForegroundColor Yellow
mvn package -DskipTests

Write-Host "`nBuild complete!" -ForegroundColor Green

Set-Location ..
