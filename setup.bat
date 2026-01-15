@echo off
REM ====================================
REM UCACUE Bar - Setup Script (Windows)
REM ====================================

setlocal EnableDelayedExpansion

echo ======================================
echo UCACUE Bar - Instalacion Automatizada
echo ======================================
echo.

REM --- Verificar Requisitos ---
echo 1. Verificando requisitos del sistema...
echo.

set MISSING_DEPS=0

where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Java encontrado
    java -version 2>&1 | findstr /i "version"
) else (
    echo [ERROR] Java no encontrado
    set MISSING_DEPS=1
)

where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Maven encontrado
) else (
    echo [ERROR] Maven no encontrado
    set MISSING_DEPS=1
)

where node >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Node.js encontrado
    node --version
) else (
    echo [ERROR] Node.js no encontrado
    set MISSING_DEPS=1
)

where npm >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] npm encontrado
) else (
    echo [ERROR] npm no encontrado
    set MISSING_DEPS=1
)

echo.

if %MISSING_DEPS% EQU 1 (
    echo [ERROR] Faltan dependencias requeridas.
    echo.
    echo Requisitos:
    echo   - Java JDK 17+
    echo   - Maven 3.9+
    echo   - Node.js 20+
    echo   - npm 10+
    echo.
    pause
    exit /b 1
)

REM --- Configurar Variables de Entorno ---
echo 2. Configurando archivos de entorno...
echo.

REM Backend .env
if not exist "backend\.env" (
    if exist "backend\.env.example" (
        copy "backend\.env.example" "backend\.env" >nul
        echo [OK] Creado backend\.env desde .env.example
        echo [AVISO] Recuerda editar backend\.env con tus credenciales
    ) else (
        echo [ERROR] No se encontro backend\.env.example
    )
) else (
    echo [AVISO] backend\.env ya existe, no se modificara
)

REM Frontend .env
if not exist "frontend\.env" (
    if exist "frontend\.env.example" (
        copy "frontend\.env.example" "frontend\.env" >nul
        echo [OK] Creado frontend\.env desde .env.example
        echo [AVISO] Recuerda editar frontend\.env con tus credenciales
    ) else (
        echo [ERROR] No se encontro frontend\.env.example
    )
) else (
    echo [AVISO] frontend\.env ya existe, no se modificara
)

echo.

REM --- Preguntar modo de instalacion ---
echo 3. Selecciona el modo de instalacion:
echo.
echo    1) Docker (recomendado) - Requiere Docker instalado
echo    2) Manual - Compilar e instalar localmente
echo    3) Solo Backend
echo    4) Solo Frontend
echo.

set /p INSTALL_MODE="Opcion [1-4]: "

echo.

if "%INSTALL_MODE%"=="1" goto :DOCKER
if "%INSTALL_MODE%"=="2" goto :MANUAL
if "%INSTALL_MODE%"=="3" goto :BACKEND
if "%INSTALL_MODE%"=="4" goto :FRONTEND

echo [ERROR] Opcion invalida
pause
exit /b 1

:DOCKER
echo 4. Instalacion con Docker...
echo.

where docker >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker no esta instalado
    pause
    exit /b 1
)

docker compose version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker Compose no esta disponible
    pause
    exit /b 1
)

echo [OK] Docker y Docker Compose encontrados
echo.

echo Iniciando contenedores...
docker compose up -d --build

echo.
echo [OK] Contenedores iniciados correctamente
echo.
echo Servicios disponibles:
echo   - Frontend: http://localhost
echo   - Backend API: http://localhost:8090/api
echo   - MySQL: localhost:3310
echo.
echo Para ver logs: docker compose logs -f
echo Para detener: docker compose down
goto :END

:MANUAL
echo 4. Instalacion manual...
echo.

echo 4.1 Compilando Backend...
cd backend
call mvn clean package -DskipTests
cd ..
echo [OK] Backend compilado

echo.
echo 4.2 Instalando dependencias del Frontend...
cd frontend
call npm install
call npm run build
cd ..
echo [OK] Frontend compilado

echo.
echo [OK] Instalacion completada
echo.
echo Para iniciar el backend:
echo   cd backend
echo   java -jar target\bar-0.0.1-SNAPSHOT.jar
echo.
echo Para iniciar el frontend (desarrollo):
echo   cd frontend
echo   npm run dev
echo.
echo Para iniciar el frontend (produccion):
echo   cd frontend
echo   npm run preview
goto :END

:BACKEND
echo 4. Compilando Backend...
echo.
cd backend
call mvn clean package -DskipTests
cd ..
echo [OK] Backend compilado
echo.
echo Para iniciar:
echo   cd backend
echo   java -jar target\bar-0.0.1-SNAPSHOT.jar
goto :END

:FRONTEND
echo 4. Instalando Frontend...
echo.
cd frontend
call npm install
echo [OK] Dependencias instaladas
echo.
echo Para iniciar en modo desarrollo:
echo   cd frontend
echo   npm run dev
echo.
echo Para compilar para produccion:
echo   cd frontend
echo   npm run build
goto :END

:END
echo.
echo ======================================
echo Instalacion completada
echo ======================================
echo.
pause
