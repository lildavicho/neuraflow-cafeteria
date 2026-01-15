# UCACUE Bar - Sistema de Gestion de Cafeteria

Sistema ERP y POS para la gestion integral de la Cafeteria UCACUE. Arquitectura basada en Spring Boot (backend), Vite/React (frontend) y MySQL (base de datos).

---

## Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Inicio Rapido con Docker](#inicio-rapido-con-docker)
3. [Instalacion Manual](#instalacion-manual)
4. [Configuracion de Variables de Entorno](#configuracion-de-variables-de-entorno)
5. [Verificacion de Servicios](#verificacion-de-servicios)
6. [Estructura del Proyecto](#estructura-del-proyecto)
7. [API Endpoints](#api-endpoints)
8. [Troubleshooting](#troubleshooting)

---

## Requisitos Previos

### Para Instalacion con Docker

| Software | Version Minima | Verificar con |
|----------|----------------|---------------|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.0+ | `docker compose version` |

### Para Instalacion Manual

| Software | Version Minima | Verificar con |
|----------|----------------|---------------|
| Java JDK | 17 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 20+ | `node --version` |
| npm | 10+ | `npm --version` |
| MySQL | 8.0 | `mysql --version` |

---

## Inicio Rapido con Docker

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/ucacue-bar.git
cd ucacue-bar
```

### 2. Configurar variables de entorno

```bash
# Backend
cp backend/.env.example backend/.env

# Frontend
cp frontend/.env.example frontend/.env
```

Editar los archivos `.env` con las credenciales apropiadas.

### 3. Iniciar todos los servicios

```bash
docker compose up -d
```

### 4. Verificar que los servicios estan funcionando

```bash
docker compose ps
```

### 5. Acceder a la aplicacion

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost/api |
| Swagger UI | http://localhost/api/swagger-ui.html |

### 6. Detener los servicios

```bash
docker compose down
```

Para eliminar tambien los volumenes de datos:

```bash
docker compose down -v
```

---

## Instalacion Manual

### 1. Configurar la Base de Datos MySQL

```sql
CREATE DATABASE ucacue_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ucacue_user'@'localhost' IDENTIFIED BY 'ucacue_pass';
GRANT ALL PRIVILEGES ON ucacue_erp.* TO 'ucacue_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configurar el Backend

```bash
cd backend
cp .env.example .env
```

Editar `backend/.env` con las credenciales de la base de datos.

Compilar y ejecutar:

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/bar-0.0.1-SNAPSHOT.jar
```

El backend estara disponible en `http://localhost:8080/api`.

### 3. Configurar el Frontend

```bash
cd frontend
cp .env.example .env
```

Editar `frontend/.env` con la URL del backend.

Instalar dependencias y ejecutar:

```bash
# Instalar dependencias
npm install

# Modo desarrollo
npm run dev

# O compilar para produccion
npm run build
npm run preview
```

El frontend estara disponible en `http://localhost:5173`.

---

## Configuracion de Variables de Entorno

### Backend (backend/.env)

| Variable | Descripcion | Valor por Defecto |
|----------|-------------|-------------------|
| DB_HOST | Host de MySQL | localhost |
| DB_PORT | Puerto de MySQL | 3306 |
| DB_NAME | Nombre de la base de datos | ucacue_erp |
| DB_USER | Usuario de MySQL | ucacue_user |
| DB_PASS | Contrasena de MySQL | ucacue_pass |
| JWT_SECRET | Secreto para tokens JWT | (requerido) |
| REDIS_HOST | Host de Redis | localhost |
| REDIS_PORT | Puerto de Redis | 6379 |
| SERVER_PORT | Puerto del servidor | 8080 |
| FIREBASE_ENABLED | Habilitar Firebase | false |
| FIREBASE_PROJECT_ID | ID del proyecto Firebase | (opcional) |
| FIREBASE_SERVICE_ACCOUNT_PATH | Ruta al archivo de servicio | (opcional) |

### Frontend (frontend/.env)

| Variable | Descripcion | Valor por Defecto |
|----------|-------------|-------------------|
| VITE_API_BASE_URL | URL base del backend | http://localhost:8080/api |
| VITE_WS_URL | URL del WebSocket | http://localhost:8080/api/ws |
| VITE_FIREBASE_API_KEY | API Key de Firebase | (opcional) |
| VITE_FIREBASE_AUTH_DOMAIN | Dominio de autenticacion | (opcional) |
| VITE_FIREBASE_PROJECT_ID | ID del proyecto | (opcional) |
| VITE_GOOGLE_CLIENT_ID | Client ID de Google OAuth | (opcional) |

---

## Verificacion de Servicios

### Backend

Verificar que el backend esta funcionando:

```bash
curl http://localhost:8080/api/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

Verificar Actuator:

```bash
curl http://localhost:8080/api/actuator/health
```

### Frontend

Abrir en el navegador: `http://localhost:5173`

Debe cargar la pagina de inicio de sesion.

### Base de Datos

Verificar conexion:

```bash
mysql -h localhost -u ucacue_user -p ucacue_erp -e "SHOW TABLES;"
```

Tablas esperadas:

- categories
- users
- products
- orders
- order_items
- sales
- loyalty_points
- loyalty_ledger
- people_counts
- people_counts_hourly
- push_tokens
- settings
- stock_movements

---

## Estructura del Proyecto

```
ucacue-bar-spring/
├── backend/                    # Aplicacion Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ucacue/bar/
│   │   │   │   ├── config/     # Configuraciones (CORS, Security, etc.)
│   │   │   │   ├── controller/ # Controladores REST
│   │   │   │   ├── dto/        # Data Transfer Objects
│   │   │   │   ├── entity/     # Entidades JPA
│   │   │   │   ├── repository/ # Repositorios JPA
│   │   │   │   ├── security/   # Configuracion JWT
│   │   │   │   └── service/    # Logica de negocio
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/   # Migraciones Flyway
│   │   └── test/
│   ├── Dockerfile
│   ├── pom.xml
│   └── .env.example
├── frontend/                   # Aplicacion Vite/React
│   ├── src/
│   │   ├── components/         # Componentes React
│   │   ├── contexts/           # Context API
│   │   ├── hooks/              # Custom hooks
│   │   ├── pages/              # Paginas
│   │   ├── services/           # Servicios API
│   │   └── utils/              # Utilidades
│   ├── Dockerfile
│   ├── package.json
│   └── .env.example
├── ops/                        # Configuraciones de operaciones
│   ├── docker-compose.yml      # Compose completo con todos los servicios
│   ├── nginx.conf              # Configuracion de Nginx
│   └── mediamtx.yml            # Configuracion de MediaMTX
├── docker-compose.yml          # Compose simplificado
├── setup.sh                    # Script de instalacion (Linux/Mac)
├── setup.bat                   # Script de instalacion (Windows)
└── README.md
```

---

## API Endpoints

### Autenticacion

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | /api/auth/login | Iniciar sesion |
| POST | /api/auth/register | Registrar usuario |
| POST | /api/auth/refresh | Renovar token |
| POST | /api/auth/logout | Cerrar sesion |

### Productos

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/products | Listar productos |
| GET | /api/products/{id} | Obtener producto |
| POST | /api/products | Crear producto |
| PUT | /api/products/{id} | Actualizar producto |
| DELETE | /api/products/{id} | Eliminar producto |

### Categorias

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/categories | Listar categorias |
| POST | /api/categories | Crear categoria |
| PUT | /api/categories/{id} | Actualizar categoria |
| DELETE | /api/categories/{id} | Eliminar categoria |

### Ordenes

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/orders | Listar ordenes |
| GET | /api/orders/{id} | Obtener orden |
| POST | /api/orders | Crear orden |
| PUT | /api/orders/{id}/status | Actualizar estado |

### Dashboard y Analiticas

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/dashboard/summary | Resumen del dashboard |
| GET | /api/analytics/sales | Datos de ventas |
| GET | /api/analytics/products | Productos mas vendidos |

### Documentacion API

Acceder a la documentacion interactiva Swagger:

```
http://localhost:8080/api/swagger-ui.html
```

---

## Puertos Utilizados

| Servicio | Puerto Interno | Puerto Externo | Descripcion |
|----------|----------------|----------------|-------------|
| MySQL | 3306 | 3310 | Base de datos |
| Redis | 6379 | 6380 | Cache |
| Backend | 8080 | 8090 | API REST |
| Frontend | 5173 | 5173 | Servidor de desarrollo |
| Nginx | 80 | 80 | Reverse proxy (produccion) |
| MediaMTX | 8554 | 8554 | RTSP streaming |
| MediaMTX | 1935 | 1935 | RTMP streaming |
| MediaMTX | 8889 | 8889 | WebRTC |

---

## Troubleshooting

### Error: Connection refused al conectar con MySQL

1. Verificar que MySQL esta ejecutandose:

```bash
# Linux/Mac
sudo systemctl status mysql

# Windows
sc query mysql
```

2. Verificar que el usuario tiene permisos:

```sql
SHOW GRANTS FOR 'ucacue_user'@'localhost';
```

3. Verificar que la base de datos existe:

```sql
SHOW DATABASES LIKE 'ucacue_erp';
```

### Error: Port 8080 already in use

Identificar y terminar el proceso que usa el puerto:

```bash
# Linux/Mac
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Error: CORS al hacer peticiones desde el frontend

Verificar que el origen del frontend esta configurado en `ALLOWED_ORIGINS`:

```yaml
# application.yml
app:
  allowed-origins: http://localhost:5173,http://localhost:5174
```

### Error: JWT token expired

Los tokens expiran por defecto en 24 horas. Usar el endpoint de refresh:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

### Error: Docker containers no inician

1. Verificar logs del contenedor:

```bash
docker compose logs <servicio>
```

2. Verificar que los puertos no estan en uso:

```bash
docker compose down
docker compose up -d
```

3. Reconstruir imagenes:

```bash
docker compose build --no-cache
docker compose up -d
```

### Error: Flyway migration failed

1. Verificar el estado de las migraciones:

```sql
SELECT * FROM flyway_schema_history;
```

2. Si hay una migracion corrupta, repararla:

```bash
cd backend
mvn flyway:repair
mvn flyway:migrate
```

### Error: Redis connection refused

1. Verificar que Redis esta funcionando:

```bash
redis-cli ping
```

2. Si no hay Redis instalado, el backend funcionara sin cache.

---

## Credenciales por Defecto (Testing)

| Campo | Valor |
|-------|-------|
| Email administrador | admin@ucacue.edu.ec |
| Contrasena | Admin123! |
| Usuario MySQL | ucacue_user |
| Contrasena MySQL | ucacue_pass |
| Base de datos | ucacue_erp |

---

## Licencia

Proyecto desarrollado para la Universidad Catolica de Cuenca (UCACUE).
