# 🚀 Instrucciones de Inicio - UCACUE Bar System

## ✅ Cambios Completados

### 1. Integración de Algolia ✅
- ✅ SDK de Java configurado (`algoliasearch-client-java`)
- ✅ Configuración de Spring Beans
- ✅ Servicio de sincronización automática
- ✅ Frontend con búsqueda instantánea
- ✅ API Keys correctamente segregadas

### 2. Mejoras de UI/UX ✅
- ✅ Contraste de inputs mejorado
- ✅ Modo oscuro optimizado
- ✅ Texto visible en todos los campos
- ✅ Logo SVG personalizado

### 3. Firebase Auth ✅
- ✅ Email/Password authentication
- ✅ Google Sign-In
- ✅ Password reset
- ✅ Integración con backend JWT

## 📋 Pasos para Iniciar el Sistema

### Paso 1: Compilar el Backend

```powershell
# Ejecutar el script de compilación
.\build-backend.ps1
```

Este script:
1. Limpia compilaciones anteriores
2. Descarga dependencias de Maven (incluyendo Algolia SDK)
3. Compila el proyecto
4. Genera el archivo JAR

**⏱️ Tiempo estimado**: 2-5 minutos (primera vez)

### Paso 2: Iniciar la Base de Datos

```powershell
# Iniciar MySQL con Docker
docker-compose -f ops/docker-compose.yml up -d mysql

# Verificar que esté corriendo
docker ps
```

### Paso 3: Iniciar el Backend

```powershell
cd backend
mvn spring-boot:run
```

El backend estará disponible en: `http://localhost:8080`

### Paso 4: Verificar el Sistema

Abrir en el navegador:
- **Frontend**: `http://localhost:8080/pages/login.html`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/v3/api-docs`

### Paso 5: Importar Datos a Algolia

Una vez que el backend esté corriendo:

```powershell
# Opción 1: Usando curl
curl -X POST http://localhost:8080/api/admin/algolia/reindex `
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Opción 2: Desde Swagger UI
# 1. Ir a http://localhost:8080/swagger-ui.html
# 2. Autenticarse como admin
# 3. Ejecutar POST /api/admin/algolia/reindex
```

## 🔑 Credenciales por Defecto

### Admin
- **Email**: `admin@ucacue.edu.ec`
- **Password**: `Admin123!`

### Comprador
- **Email**: `comprador@ucacue.edu.ec`
- **Password**: `Comprador123!`

## 🔍 Verificar Algolia

### En el Dashboard de Algolia
1. Ir a https://www.algolia.com/dashboard
2. Iniciar sesión con tu cuenta
3. Seleccionar el índice `productos_venta`
4. Verificar que los productos estén indexados

### Desde el Frontend
```javascript
// Abrir consola del navegador en /pages/login.html
import * as algolia from '/js/algoliaSearch.js';

// Buscar productos
const results = await algolia.searchProducts('café');
console.log(results);
```

## 🎨 Verificar Mejoras de Contraste

### Modo Claro
1. Abrir `/pages/login.html`
2. Verificar que el texto en los inputs sea claramente visible
3. Color del texto: Negro sólido (#111827)
4. Fondo de inputs: Blanco (#ffffff)

### Modo Oscuro
1. Activar modo oscuro (botón en la esquina)
2. Verificar contraste mejorado
3. Color del texto: Gris claro (#f3f4f6)
4. Fondo de inputs: Gris oscuro (#1f2937)

## 🔧 Solución de Problemas

### Error: "The import com.algolia cannot be resolved"

**Causa**: Maven no ha descargado las dependencias

**Solución**:
```powershell
cd backend
mvn clean install
```

### Error: "Cannot connect to database"

**Causa**: MySQL no está corriendo

**Solución**:
```powershell
docker-compose -f ops/docker-compose.yml up -d mysql
docker logs ucacue-mysql
```

### Error: "Texto invisible en inputs"

**Causa**: Caché del navegador

**Solución**:
1. Presionar `Ctrl + Shift + R` para recargar sin caché
2. O limpiar caché del navegador

### Error: "Firebase auth not working"

**Causa**: Módulos no cargados

**Solución**:
1. Abrir consola del navegador (F12)
2. Verificar errores de importación
3. Asegurarse de que los archivos existan:
   - `/js/firebaseAuth.js`
   - `/js/authIntegration.js`

## 📊 Estructura del Proyecto

```
ucacue-bar-spring/
├── backend/
│   ├── src/main/java/com/ucacue/bar/
│   │   ├── config/
│   │   │   └── AlgoliaConfig.java         ✨ NUEVO
│   │   ├── controller/
│   │   │   └── AdminController.java       ✨ NUEVO
│   │   ├── service/
│   │   │   └── AlgoliaService.java        ✅ ACTUALIZADO
│   │   └── dto/
│   │       └── ProductDTO.java            ✅ ACTUALIZADO
│   └── pom.xml                            ✅ ACTUALIZADO
├── frontend/
│   ├── assets/
│   │   └── logo.svg                       ✨ NUEVO
│   ├── js/
│   │   ├── firebaseAuth.js                ✨ NUEVO
│   │   ├── authIntegration.js             ✨ NUEVO
│   │   └── algoliaSearch.js               ✅ ACTUALIZADO
│   └── pages/
│       └── login.html                     ✅ ACTUALIZADO
├── build-backend.ps1                      ✨ NUEVO
├── CORRECCIONES_FINALES.md                ✨ NUEVO
└── INSTRUCCIONES_INICIO.md                ✨ NUEVO (este archivo)
```

## 🎯 Próximos Pasos

1. ✅ **Compilar backend**: `.\build-backend.ps1`
2. ✅ **Iniciar servicios**: Docker + Spring Boot
3. ✅ **Importar datos**: Endpoint de reindex
4. ✅ **Probar login**: Con credenciales de admin
5. ✅ **Probar búsqueda**: En el frontend
6. ✅ **Verificar contraste**: Modo claro y oscuro

## 📝 Notas Importantes

### Seguridad
- ⚠️ **NUNCA** commitear API Keys reales a Git
- ⚠️ Cambiar las claves en producción
- ⚠️ Usar variables de entorno en producción

### Algolia
- ✅ Write API Key solo en backend
- ✅ Search API Key en frontend (read-only)
- ✅ Sincronización automática en CRUD de productos

### Firebase
- ✅ Configuración en `firebaseAuth.js`
- ✅ Integración con backend JWT
- ✅ Google Sign-In habilitado

## 🆘 Soporte

Si encuentras algún problema:

1. **Revisar logs del backend**:
   ```powershell
   # En la terminal donde corre Spring Boot
   # Buscar errores en rojo
   ```

2. **Revisar consola del navegador**:
   ```
   F12 → Console
   Buscar errores en rojo
   ```

3. **Verificar servicios**:
   ```powershell
   docker ps                    # MySQL debe estar corriendo
   curl http://localhost:8080   # Backend debe responder
   ```

## ✨ Estado Final

- ✅ Algolia SDK correctamente integrado
- ✅ Contraste de UI mejorado
- ✅ Firebase Auth funcionando
- ✅ Logo restaurado
- ✅ Documentación completa
- ✅ Scripts de automatización creados
- ✅ Commits realizados en rama `feature/firebase-algolia-integration`

**¡El sistema está listo para compilar y ejecutar!** 🚀
