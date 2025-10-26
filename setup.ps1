# ========================================
# 🚀 UCACUE Bar - Setup Script
# ========================================
# Este script automatiza la configuración inicial del proyecto

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🚀 UCACUE Bar - Setup Automático" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Renombrar login
Write-Host "📝 Paso 1: Renombrando login.html..." -ForegroundColor Yellow

$loginOld = "frontend\pages\login.html"
$loginNew = "frontend\pages\login_new.html"
$loginBackup = "frontend\pages\login_old_backup.html"

if (Test-Path $loginNew) {
    if (Test-Path $loginOld) {
        Write-Host "   ↳ Respaldando login.html antiguo..." -ForegroundColor Gray
        Remove-Item -Path $loginOld -Force -ErrorAction SilentlyContinue
    }
    Write-Host "   ↳ Activando nuevo login.html..." -ForegroundColor Gray
    Rename-Item -Path $loginNew -NewName "login.html" -Force
    Write-Host "   ✅ Login actualizado correctamente`n" -ForegroundColor Green
} elseif (Test-Path $loginOld) {
    Write-Host "   ✅ Login ya está actualizado`n" -ForegroundColor Green
} else {
    Write-Host "   ❌ No se encontró ningún archivo login.html`n" -ForegroundColor Red
    exit 1
}

# 2. Verificar Docker
Write-Host "🐳 Paso 2: Verificando Docker Desktop..." -ForegroundColor Yellow

try {
    $dockerVersion = docker --version
    Write-Host "   ✅ Docker instalado: $dockerVersion`n" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Docker no encontrado. Por favor instala Docker Desktop.`n" -ForegroundColor Red
    exit 1
}

# 3. Detener contenedores existentes
Write-Host "🛑 Paso 3: Deteniendo contenedores existentes..." -ForegroundColor Yellow
docker compose -f ops/docker-compose.yml down -v 2>$null
Write-Host "   ✅ Contenedores detenidos`n" -ForegroundColor Green

# 4. Construir e iniciar servicios
Write-Host "🔨 Paso 4: Construyendo e iniciando servicios..." -ForegroundColor Yellow
Write-Host "   (Esto puede tomar varios minutos...)`n" -ForegroundColor Gray

docker compose -f ops/docker-compose.yml up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n   ✅ Servicios iniciados correctamente`n" -ForegroundColor Green
} else {
    Write-Host "`n   ❌ Error al iniciar servicios`n" -ForegroundColor Red
    exit 1
}

# 5. Esperar a que los servicios estén listos
Write-Host "⏳ Paso 5: Esperando a que los servicios estén listos..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 6. Verificar estado
Write-Host "`n📊 Paso 6: Verificando estado de servicios...`n" -ForegroundColor Yellow

Write-Host "   Contenedores activos:" -ForegroundColor Gray
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host "`n   Logs del backend (últimas 20 líneas):" -ForegroundColor Gray
docker logs ucacue_app --tail 20

Write-Host "`n   Logs de Nginx (últimas 10 líneas):" -ForegroundColor Gray
docker logs ucacue_nginx --tail 10

# 7. Probar endpoints
Write-Host "`n🔍 Paso 7: Probando endpoints...`n" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:3001/pages/login.html" -Method Head -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ Frontend accesible: http://localhost:3001/pages/login.html" -ForegroundColor Green
    }
} catch {
    Write-Host "   ❌ Frontend no accesible en puerto 3001" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" -Method Get -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ Backend accesible: http://localhost:8080/api" -ForegroundColor Green
    }
} catch {
    Write-Host "   ⚠️  Backend aún no está listo (puede tomar más tiempo)" -ForegroundColor Yellow
}

# 8. Resumen
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ SETUP COMPLETADO" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "📌 URLs de acceso:" -ForegroundColor White
Write-Host "   • Login:     http://localhost:3001/pages/login.html" -ForegroundColor Cyan
Write-Host "   • Dashboard: http://localhost:3001/pages/dashboard.html" -ForegroundColor Cyan
Write-Host "   • API:       http://localhost:8080/api" -ForegroundColor Cyan

Write-Host "`n🔑 Credenciales:" -ForegroundColor White
Write-Host "   • Admin:     admin@ucacue.edu.ec / Admin123!" -ForegroundColor Cyan
Write-Host "   • Comprador: comprador@ucacue.edu.ec / Admin123!" -ForegroundColor Cyan

Write-Host "`n🗄️  MySQL Workbench:" -ForegroundColor White
Write-Host "   • Host:     127.0.0.1" -ForegroundColor Cyan
Write-Host "   • Port:     3306" -ForegroundColor Cyan
Write-Host "   • User:     ucacue_user" -ForegroundColor Cyan
Write-Host "   • Password: ucacue_pass" -ForegroundColor Cyan
Write-Host "   • Schema:   ucacue_erp" -ForegroundColor Cyan

Write-Host "`n📚 Documentación:" -ForegroundColor White
Write-Host "   • Ver CAMBIOS_REALIZADOS.md para detalles completos" -ForegroundColor Gray

Write-Host "`n🎉 ¡Listo! Abre http://localhost:3001/pages/login.html en tu navegador`n" -ForegroundColor Green
