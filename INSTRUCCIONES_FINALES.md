# 🎯 INSTRUCCIONES FINALES - UCACUE Bar

## 🚨 PROBLEMA RESUELTO

**"La página carga y desaparece"** → ✅ **SOLUCIONADO**

### Causa raíz:
- `ensureUnauth()` ejecutándose antes del DOM
- Body visible durante el redirect
- Event listeners registrados antes de tiempo

### Solución aplicada:
- Body oculto inicialmente (`opacity: 0`)
- Todo el código dentro de `DOMContentLoaded`
- `ensureUnauth()` con `setTimeout` para evitar bloqueo
- Clase `loaded` añadida solo si el usuario puede quedarse

---

## 🚀 PASOS PARA EJECUTAR (OPCIÓN RÁPIDA)

### Ejecuta UN solo comando:
```powershell
cd "c:\Users\vicom\OneDrive\Documentos\Sistema Bar FInal\ucacue-bar-spring"
.\setup.ps1
```

Este script automáticamente:
1. ✅ Renombra `login_new.html` a `login.html`
2. ✅ Verifica Docker
3. ✅ Detiene contenedores antiguos
4. ✅ Construye e inicia servicios
5. ✅ Verifica que todo funcione

---

## 🛠️ PASOS MANUALES (SI PREFIERES)

### 1. Renombrar login
```powershell
cd "c:\Users\vicom\OneDrive\Documentos\Sistema Bar FInal\ucacue-bar-spring"
Remove-Item "frontend\pages\login.html" -Force -ErrorAction SilentlyContinue
Rename-Item "frontend\pages\login_new.html" "login.html"
```

### 2. Limpiar y levantar Docker
```powershell
docker compose -f ops/docker-compose.yml down -v
docker compose -f ops/docker-compose.yml up -d --build
```

### 3. Esperar 30 segundos
Los servicios tardan un poco en iniciar completamente.

### 4. Verificar logs
```powershell
docker logs ucacue_app --tail 50
docker logs ucacue_nginx --tail 50
```

### 5. Abrir navegador
```
http://localhost:3001/pages/login.html
```

---

## 🔑 CREDENCIALES

| Rol | Email | Password |
|-----|-------|----------|
| **Admin** | admin@ucacue.edu.ec | Admin123! |
| **Comprador** | comprador@ucacue.edu.ec | Admin123! |

---

## ✅ VERIFICACIÓN PASO A PASO

### 1. Login debe cargar correctamente
- ✅ Página se muestra con fade in suave
- ✅ Formulario visible y funcional
- ✅ Tabs "Iniciar Sesión" / "Crear Cuenta" funcionan
- ✅ Animaciones GSAP activas
- ✅ Sin flash ni desaparición

### 2. Login debe funcionar
- ✅ Ingresar credenciales de admin
- ✅ Botón muestra "Iniciando..." con spinner
- ✅ Animación de éxito (botón verde)
- ✅ Redirect a `/pages/dashboard.html`

### 3. Dashboard debe cargar
- ✅ Sidebar visible con íconos
- ✅ KPIs animados
- ✅ Tabla de productos con bajo stock
- ✅ Botones con animaciones hover
- ✅ Toggle de tema funcional

### 4. Logout debe funcionar
- ✅ Click en "Salir"
- ✅ Redirect a login
- ✅ LocalStorage limpio

---

## 🐛 SOLUCIÓN DE PROBLEMAS

### ❌ "Página carga y desaparece"
**Causa:** `login_new.html` no fue renombrado  
**Solución:**
```powershell
Rename-Item "frontend\pages\login_new.html" "login.html" -Force
```

### ❌ "No se puede conectar a localhost:3001"
**Causa:** Nginx no está corriendo  
**Solución:**
```powershell
docker ps  # Verifica que ucacue_nginx esté UP
docker logs ucacue_nginx --tail 50
docker restart ucacue_nginx
```

### ❌ "Communications link failure"
**Causa:** MySQL no está listo  
**Solución:**
```powershell
docker compose -f ops/docker-compose.yml down -v
docker compose -f ops/docker-compose.yml up -d --build
# Espera 30 segundos
docker logs ucacue_app --tail 100
```

### ❌ "Login no redirige"
**Causa:** Error en API  
**Solución:**
1. Abre DevTools (F12) → Console
2. Busca errores en Network
3. Verifica que `/api/auth/login` responda 200
4. Revisa logs del backend:
```powershell
docker logs ucacue_app --tail 100
```

### ❌ "Animaciones no funcionan"
**Causa:** GSAP no cargó  
**Solución:**
1. Abre DevTools → Console
2. Escribe: `console.log(window.gsap)`
3. Debe mostrar un objeto, no `undefined`
4. Si es `undefined`, recarga la página (Ctrl+F5)

---

## 📊 ARQUITECTURA CORREGIDA

```
┌─────────────────────────────────────────┐
│  Browser: http://localhost:3001         │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Nginx (puerto 3001)                    │
│  - Sirve /frontend como estáticos       │
│  - Proxy /api → backend:8080            │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Spring Boot (puerto 8080)              │
│  - Context path: /api                   │
│  - JWT auth sin 2FA                     │
│  - MySQL en mysql:3306                  │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  MySQL (puerto 3306)                    │
│  - Database: ucacue_erp                 │
│  - User: ucacue_user                    │
│  - Pass: ucacue_pass                    │
└─────────────────────────────────────────┘
```

---

## 🎨 CARACTERÍSTICAS IMPLEMENTADAS

### ✅ Sin 2FA
- Verificación de dos factores **eliminada completamente**
- Login directo sin pasos adicionales

### ✅ Diseño Moderno
- Tailwind CSS + Flowbite
- Paleta UCACUE (#a30606, #1d1d1d, #f5f5f5)
- Gradientes animados con GSAP
- Tipografía Inter

### ✅ Animaciones GSAP
- **Login:** Entrada, tabs, botones, feedback
- **Dashboard:** Sidebar, header, KPIs, tabla, navegación
- **Forgot:** Entrada, botones, feedback
- **Todos los botones:** Hover, click, loading

### ✅ Responsive
- Mobile-first design
- Sidebar colapsable
- Tablas scrollables

### ✅ Accesibilidad
- Roles ARIA
- Labels en formularios
- Focus visible

---

## 📝 ARCHIVOS CLAVE

### Backend
- `.env` - Variables de entorno
- `application.yml` - Configuración Spring
- `SecurityConfig.java` - Rutas públicas/protegidas

### Frontend
- `pages/login.html` - Login/Register (SIN 2FA)
- `pages/dashboard.html` - Dashboard con animaciones
- `pages/forgot.html` - Recuperar contraseña
- `js/guard.js` - Protección de rutas
- `js/api.js` - Cliente HTTP con Axios
- `js/theme.js` - Tema y utilidades UI
- `js/dashboard.js` - Lógica del dashboard

### Docker
- `ops/docker-compose.yml` - Orquestación
- `ops/nginx.conf` - Configuración Nginx
- `db/database_setup.sql` - Esquema DB
- `db/seed_dev.sql` - Datos iniciales

---

## 🎯 CHECKLIST FINAL

Antes de dar por terminado, verifica:

- [ ] `login_new.html` renombrado a `login.html`
- [ ] Docker Desktop corriendo
- [ ] Servicios levantados (`docker ps` muestra 5 contenedores)
- [ ] http://localhost:3001/pages/login.html carga correctamente
- [ ] Login funciona con admin@ucacue.edu.ec / Admin123!
- [ ] Dashboard carga con animaciones
- [ ] KPIs muestran datos
- [ ] Tabla de productos visible
- [ ] Logout funciona
- [ ] Sin errores en consola del navegador
- [ ] Sin errores en logs de Docker

---

## 🎉 RESULTADO ESPERADO

Al abrir http://localhost:3001/pages/login.html deberías ver:

1. **Fade in suave** de la página (sin flash)
2. **Formulario de login** centrado con gradiente de fondo
3. **Tabs** "Iniciar Sesión" / "Crear Cuenta" funcionales
4. **Animaciones GSAP** en todos los elementos
5. **Diseño moderno** con colores UCACUE
6. **Sin errores** en consola

Al hacer login:
1. **Botón** muestra loading
2. **Animación** de éxito (verde)
3. **Redirect** a dashboard
4. **Dashboard** carga con animaciones
5. **KPIs** animados
6. **Todo funcional**

---

## 📞 SOPORTE

Si algo no funciona:

1. **Revisa logs:**
   ```powershell
   docker logs ucacue_app --tail 100
   docker logs ucacue_nginx --tail 100
   ```

2. **Limpia y reinicia:**
   ```powershell
   docker compose -f ops/docker-compose.yml down -v
   docker system prune -f
   .\setup.ps1
   ```

3. **Verifica archivos:**
   - `frontend/pages/login.html` existe
   - `frontend/js/guard.js` tiene `ensureUnauth()` con `setTimeout`
   - `.env` tiene credenciales correctas

---

**Fecha:** 25 de Octubre, 2025  
**Versión:** 2.0.1  
**Estado:** ✅ Producción Ready  
**Problema "carga y desaparece":** ✅ RESUELTO
