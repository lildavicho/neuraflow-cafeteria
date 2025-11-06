# UCACUE Bar System

Sistema integral de gestión de cafetería para la Universidad Católica de Cuenca.

## 🚀 Características Principales

### Roles y Permisos
- **ADMIN**: Dueños de la cafetería con acceso completo al sistema
- **COMPRADOR**: Estudiantes/Profesores/Personal con acceso a POS y perfil

### Funcionalidades

#### Para Administradores:
- ✅ Dashboard con métricas en tiempo real (SSE)
- ✅ Gestión completa de inventario
- ✅ Reportes y exportación (CSV, XLSX, PDF)
- ✅ Monitoreo con cámaras (WebRTC)
- ✅ Gestión de usuarios y configuraciones
- ✅ Algoritmos predictivos (demanda, anomalías)

#### Para Compradores:
- ✅ Punto de venta (POS) intuitivo
- ✅ Múltiples métodos de pago (Efectivo, PayPhone, Datafast, QR)
- ✅ Historial de compras
- ✅ Búsqueda instantánea con Algolia

### Tecnologías

#### Backend
- Spring Boot 3.3 con Java 17
- MySQL 8.0
- JWT para autenticación
- Firebase Auth
- Algolia para búsqueda
- WebRTC para streaming

#### Frontend
- HTML5 + Bootstrap 5
- GSAP para animaciones
- Chart.js para gráficos
- Tabler UI components

## 📦 Instalación

### Requisitos
- Docker y Docker Compose
- Java 17 (para desarrollo local)
- Node.js 16+ (para desarrollo frontend)
- MySQL 8.0

### Configuración

1. **Clonar el repositorio**
```bash
git clone https://github.com/ucacue/bar-spring.git
cd ucacue-bar-spring
```

2. **Configurar variables de entorno**
```bash
cp .env.example .env
# Editar .env con tus credenciales
```

3. **Configurar Firebase**
   - Crear proyecto en Firebase Console
   - Habilitar Authentication
   - Descargar `serviceAccount.json`
   - Actualizar configuración en frontend

4. **Configurar Algolia**
   - Crear cuenta en Algolia
   - Crear índice `products`
   - Actualizar credenciales en `.env`

### Ejecución con Docker

```bash
# Construir e iniciar servicios
docker-compose -f ops/docker-compose.yml up -d

# Ver logs
docker-compose -f ops/docker-compose.yml logs -f app

# Detener servicios
docker-compose -f ops/docker-compose.yml down
```

### Ejecución Local (Desarrollo)

#### Backend:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Frontend:
```bash
cd frontend
# Servir con cualquier servidor HTTP
python -m http.server 3001
# o
npx serve -p 3001
```

## 🔑 Usuarios por Defecto

| Email | Contraseña | Rol | Descripción |
|-------|------------|-----|-------------|
| admin@ucacue.edu.ec | Admin123! | ADMIN | Administrador del sistema |
| comprador@ucacue.edu.ec | Comprador123! | COMPRADOR | Usuario de prueba |

## 📊 API Endpoints

### Autenticación
- `POST /api/auth/login` - Login nativo
- `POST /api/auth/register` - Registro con validación de cédula
- `POST /api/auth/firebase` - Login con Firebase
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/2fa/verify` - Verificación 2FA

### Productos
- `GET /api/products` - Listar productos (paginado)
- `GET /api/products/public` - Lista pública para POS
- `POST /api/products` - Crear producto (ADMIN)
- `PUT /api/products/{id}` - Actualizar producto (ADMIN)
- `GET /api/products/low-stock` - Productos con stock bajo (ADMIN)

### Ventas
- `POST /api/sales` - Crear venta (con Idempotency-Key)
- `GET /api/sales/mine` - Historial del comprador
- `GET /api/sales` - Todas las ventas (ADMIN)
- `GET /api/sales/summary` - Resumen de ventas

### Dashboard
- `GET /api/dashboard/metrics/sse` - Métricas en tiempo real (SSE)

### Algoritmos
- `GET /api/algo/recommendations` - Recomendaciones de productos
- `GET /api/algo/forecast` - Predicción de demanda
- `GET /api/algo/anomalies` - Detección de anomalías
- `GET /api/algo/clusters` - Clustering de clientes

## 🎥 Streaming de Video

El sistema integra MediaMTX para streaming WebRTC de cámaras IP:

1. Configurar cámaras RTSP en `ops/mediamtx.yml`
2. Acceder al stream via WebRTC en `/api/camera/webrtc/source`
3. Dashboard muestra feeds en tiempo real

## 💳 Integraciones de Pago

### PayPhone
- Sandbox configurado por defecto
- Actualizar `PAYPHONE_KEY` en producción

### Datafast
- Integración con API REST
- Configurar `DATAFAST_KEY` para producción

### QR Payments
- Generación de códigos QR con ZXing
- Compatible con billeteras móviles

## 🔒 Seguridad

- Autenticación JWT con refresh tokens
- Firebase Auth como alternativa
- 2FA por email
- Rate limiting (5 req/min para login)
- Validación de cédula ecuatoriana
- Headers de seguridad (CSP, HSTS)
- CORS configurado

## 📈 Algoritmos Implementados

### Recomendación de Productos
- Análisis de co-ocurrencia
- Reglas de negocio (café → snacks)
- Personalización por usuario

### Predicción de Demanda
- Suavizado exponencial simple
- Análisis de tendencias
- Factores estacionales

### Detección de Anomalías
- Z-score por hora/día
- Alertas automáticas

### Clustering de Clientes
- K-Means con K=3
- Variables RFM simplificadas

## 🚀 Despliegue en Producción

Ver [oracle-cloud-deploy.md](oracle-cloud-deploy.md) para instrucciones detalladas.

### Checklist de Producción

- [ ] Cambiar `DDL_AUTO` a `validate` o `none`
- [ ] Configurar SSL/HTTPS
- [ ] Actualizar credenciales de producción
- [ ] Configurar backups automáticos
- [ ] Habilitar monitoreo (Prometheus/Grafana)
- [ ] Configurar logs centralizados
- [ ] Actualizar CORS para dominio de producción

## 📝 Documentación Adicional

- [API Contracts](API_CONTRACTS.md)
- [Security Guide](SECURITY.md)
- [Streaming Setup](STREAMING.md)
- [Algolia Integration](ALGOLIA.md)
- [Roadmap](ROADMAP.md)

## 🤝 Contribución

1. Fork el proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📄 Licencia

Propiedad de Universidad Católica de Cuenca - Todos los derechos reservados.

## 📞 Soporte

Para soporte técnico, contactar:
- Email: soporte@ucacue.edu.ec
- Tel: (07) 2831608

---

Desarrollado con ❤️ para UCACUE
