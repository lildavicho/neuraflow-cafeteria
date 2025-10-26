# 🚀 UCACUE Bar - Quick Start Guide

## ⚡ Inicio Rápido (1 comando)

```powershell
# Desde la raíz del proyecto:
.\setup.ps1
```

Este script automáticamente:
- ✅ Renombra el nuevo login
- ✅ Detiene contenedores antiguos
- ✅ Construye e inicia todos los servicios
- ✅ Verifica que todo funcione correctamente

---

## 🔧 Inicio Manual

### 1. Renombrar login
```powershell
Remove-Item "frontend\pages\login.html" -Force
Rename-Item "frontend\pages\login_new.html" "login.html"
```

### 2. Levantar servicios
```powershell
docker compose -f ops/docker-compose.yml down -v
docker compose -f ops/docker-compose.yml up -d --build
```

### 3. Verificar
```powershell
docker ps
docker logs ucacue_app --tail 50
docker logs ucacue_nginx --tail 50
```

### 4. Abrir en navegador
http://localhost:3001/pages/login.html

---

## 🔑 Credenciales

| Rol | Email | Password |
|-----|-------|----------|
| **Admin** | admin@ucacue.edu.ec | Admin123! |
| **Comprador** | comprador@ucacue.edu.ec | Admin123! |

---

## 🌐 URLs

| Servicio | URL |
|----------|-----|
| **Login** | http://localhost:3001/pages/login.html |
| **Dashboard** | http://localhost:3001/pages/dashboard.html |
| **API** | http://localhost:8080/api |
| **Swagger** | http://localhost:8080/api/swagger-ui.html |
| **Health** | http://localhost:8080/api/actuator/health |

---

## 🗄️ MySQL Workbench

```
Host:     127.0.0.1
Port:     3306
User:     ucacue_user
Password: ucacue_pass
Schema:   ucacue_erp
```

---

## ✨ Características Nuevas

### ❌ Sin 2FA
- Verificación de dos factores **eliminada completamente**
- Login directo sin pasos adicionales

### 🎨 Diseño Moderno
- Tailwind CSS + Flowbite
- Paleta UCACUE (#a30606, #1d1d1d, #f5f5f5)
- Gradientes animados con GSAP

### 🎭 Animaciones GSAP
- **Login/Register:** Entrada, tabs, botones, feedback
- **Dashboard:** Sidebar, header, KPIs, tabla, navegación
- **Forgot:** Entrada, botones, feedback
- **Todos los botones:** Hover, click, loading states

### 📱 Responsive
- Mobile-first design
- Sidebar colapsable
- Tablas scrollables

---

## 🐛 Problemas Comunes

### ❌ "No se puede conectar" en localhost:3001
**Solución:**
```powershell
docker ps  # Verifica que ucacue_nginx esté corriendo
docker logs ucacue_nginx --tail 50
```

### ❌ "Communications link failure"
**Solución:**
```powershell
# Reinicia los servicios con volúmenes limpios
docker compose -f ops/docker-compose.yml down -v
docker compose -f ops/docker-compose.yml up -d --build
```

### ❌ Login no redirige
**Solución:**
- Abre DevTools (F12) → Console
- Verifica errores de red
- Confirma que `/api/auth/login` responde 200

---

## 📚 Documentación Completa

- **CAMBIOS_REALIZADOS.md** - Lista completa de cambios
- **frontend/README.md** - Documentación del frontend
- **ops/README.md** - Configuración de Docker

---

## 🎯 Flujo de Usuario

1. **Abrir** http://localhost:3001/pages/login.html
2. **Iniciar sesión** con admin@ucacue.edu.ec / Admin123!
3. **Redirigido** automáticamente a /pages/dashboard.html
4. **Ver KPIs** animados y tabla de productos con bajo stock
5. **Navegar** a POS, Inventario, etc.

---

## 🔄 Comandos Útiles

```powershell
# Ver logs en tiempo real
docker logs -f ucacue_app

# Reiniciar solo el backend
docker restart ucacue_app

# Reiniciar solo Nginx
docker restart ucacue_nginx

# Ver todos los contenedores
docker ps -a

# Eliminar todo y empezar de cero
docker compose -f ops/docker-compose.yml down -v
docker system prune -a
.\setup.ps1
```

---

## ✅ Checklist de Verificación

- [ ] Docker Desktop corriendo
- [ ] Puerto 3001 libre
- [ ] Puerto 8080 libre
- [ ] Puerto 3306 libre
- [ ] `login_new.html` renombrado a `login.html`
- [ ] Servicios levantados con `docker compose up`
- [ ] Login accesible en http://localhost:3001/pages/login.html
- [ ] Backend responde en http://localhost:8080/api/actuator/health
- [ ] Login funciona con credenciales de admin
- [ ] Dashboard carga correctamente

---

## 🎉 ¡Listo!

Si todos los pasos anteriores funcionan, el sistema está **100% operativo**.

**Disfruta del nuevo diseño con animaciones GSAP y sin errores de 2FA! 🚀**
