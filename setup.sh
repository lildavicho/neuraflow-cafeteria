#!/bin/bash
# ====================================
# UCACUE Bar - Setup Script (Linux/Mac)
# ====================================

set -e

echo "======================================"
echo "UCACUE Bar - Instalacion Automatizada"
echo "======================================"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funciones de utilidad
print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[AVISO]${NC} $1"
}

check_command() {
    if command -v $1 &> /dev/null; then
        print_success "$1 encontrado: $($1 --version 2>&1 | head -n 1)"
        return 0
    else
        print_error "$1 no encontrado"
        return 1
    fi
}

# --- Verificar Requisitos ---
echo "1. Verificando requisitos del sistema..."
echo ""

MISSING_DEPS=0

if ! check_command java; then
    MISSING_DEPS=1
fi

if ! check_command mvn; then
    MISSING_DEPS=1
fi

if ! check_command node; then
    MISSING_DEPS=1
fi

if ! check_command npm; then
    MISSING_DEPS=1
fi

echo ""

if [ $MISSING_DEPS -eq 1 ]; then
    print_error "Faltan dependencias requeridas. Por favor instalarlas antes de continuar."
    echo ""
    echo "Requisitos:"
    echo "  - Java JDK 17+"
    echo "  - Maven 3.9+"
    echo "  - Node.js 20+"
    echo "  - npm 10+"
    exit 1
fi

# --- Configurar Variables de Entorno ---
echo "2. Configurando archivos de entorno..."
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Backend .env
if [ ! -f backend/.env ]; then
    if [ -f backend/.env.example ]; then
        cp backend/.env.example backend/.env
        print_success "Creado backend/.env desde .env.example"
        print_warning "Recuerda editar backend/.env con tus credenciales"
    else
        print_error "No se encontro backend/.env.example"
    fi
else
    print_warning "backend/.env ya existe, no se modificara"
fi

# Frontend .env
if [ ! -f frontend/.env ]; then
    if [ -f frontend/.env.example ]; then
        cp frontend/.env.example frontend/.env
        print_success "Creado frontend/.env desde .env.example"
        print_warning "Recuerda editar frontend/.env con tus credenciales"
    else
        print_error "No se encontro frontend/.env.example"
    fi
else
    print_warning "frontend/.env ya existe, no se modificara"
fi

echo ""

# --- Preguntar modo de instalacion ---
echo "3. Selecciona el modo de instalacion:"
echo ""
echo "   1) Docker (recomendado) - Requiere Docker instalado"
echo "   2) Manual - Compilar e instalar localmente"
echo "   3) Solo Backend"
echo "   4) Solo Frontend"
echo ""

read -p "Opcion [1-4]: " INSTALL_MODE

echo ""

case $INSTALL_MODE in
    1)
        # --- Instalacion Docker ---
        echo "4. Instalacion con Docker..."
        echo ""
        
        if ! check_command docker; then
            print_error "Docker no esta instalado"
            exit 1
        fi
        
        if ! docker compose version &> /dev/null; then
            print_error "Docker Compose no esta disponible"
            exit 1
        fi
        
        print_success "Docker y Docker Compose encontrados"
        echo ""
        
        echo "Iniciando contenedores..."
        docker compose up -d --build
        
        echo ""
        print_success "Contenedores iniciados correctamente"
        echo ""
        echo "Servicios disponibles:"
        echo "  - Frontend: http://localhost"
        echo "  - Backend API: http://localhost:8090/api"
        echo "  - MySQL: localhost:3310"
        echo ""
        echo "Para ver logs: docker compose logs -f"
        echo "Para detener: docker compose down"
        ;;
        
    2)
        # --- Instalacion Manual Completa ---
        echo "4. Instalacion manual..."
        echo ""
        
        # Backend
        echo "4.1 Compilando Backend..."
        cd backend
        mvn clean package -DskipTests
        cd ..
        print_success "Backend compilado"
        
        # Frontend
        echo ""
        echo "4.2 Instalando dependencias del Frontend..."
        cd frontend
        npm install
        npm run build
        cd ..
        print_success "Frontend compilado"
        
        echo ""
        print_success "Instalacion completada"
        echo ""
        echo "Para iniciar el backend:"
        echo "  cd backend && java -jar target/bar-0.0.1-SNAPSHOT.jar"
        echo ""
        echo "Para iniciar el frontend (desarrollo):"
        echo "  cd frontend && npm run dev"
        echo ""
        echo "Para iniciar el frontend (produccion):"
        echo "  cd frontend && npm run preview"
        ;;
        
    3)
        # --- Solo Backend ---
        echo "4. Compilando Backend..."
        echo ""
        cd backend
        mvn clean package -DskipTests
        cd ..
        print_success "Backend compilado"
        echo ""
        echo "Para iniciar:"
        echo "  cd backend && java -jar target/bar-0.0.1-SNAPSHOT.jar"
        ;;
        
    4)
        # --- Solo Frontend ---
        echo "4. Instalando Frontend..."
        echo ""
        cd frontend
        npm install
        print_success "Dependencias instaladas"
        echo ""
        echo "Para iniciar en modo desarrollo:"
        echo "  cd frontend && npm run dev"
        echo ""
        echo "Para compilar para produccion:"
        echo "  cd frontend && npm run build"
        ;;
        
    *)
        print_error "Opcion invalida"
        exit 1
        ;;
esac

echo ""
echo "======================================"
echo "Instalacion completada"
echo "======================================"
