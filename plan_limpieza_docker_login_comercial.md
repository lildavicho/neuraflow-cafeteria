# PLAN MAESTRO
# Limpieza del proyecto, variables seguras, Docker escalable, login privado y flujo comercial publico

## 0. Decision de modelo para ejecutar

### Modelo principal

Usar:

```text
Codex con GPT-5.5 dentro de WebStorm / IntelliJ IDEA
```

Motivo:

```text
Debe leer el repositorio real, modificar archivos, crear Dockerfiles, ajustar .gitignore, revisar variables, corregir login, ejecutar tests y dejar el proyecto listo para deploy.
```

### Modelo para revisar arquitectura

Usar:

```text
GPT-5.5 en ChatGPT
```

Uso:

```text
Revisar plan, estructura, decisiones de login, tenant, seguridad, deploy y flujo comercial.
```

### Modelo para auditoria final

Usar:

```text
Opus 4.7
```

Uso:

```text
Auditoria de seguridad, variables, Docker, login, multi-tenant, proteccion de rutas y riesgos de produccion.
```

---

## 1. Objetivo general

Dejar el proyecto completamente limpio, seguro, dockerizado, escalable y preparado para subir a cualquier proveedor sin rehacer la configuracion cada vez.

El proyecto debe quedar listo para:

```text
- Ejecutarse localmente con Docker Compose.
- Desplegarse en VPS, Render, Railway, Fly.io, DigitalOcean, AWS, GCP o cualquier proveedor con Docker.
- Mantener variables protegidas.
- Evitar secretos en Git.
- Tener .gitignore completo.
- Separar frontend, backend y base de datos.
- Tener login privado para usuarios creados/autorizados.
- Tener pagina publica comercial para interesados.
- Tener flujo de contacto/compra/demo.
- Mantener multi-tenant seguro.
- Mantener Vision AI, SRI, Resend y dashboards funcionando.
```

---

## 2. Contexto real del proyecto

Datos detectados previamente:

```text
Backend: Spring Boot 3.5.7
Lenguaje backend: Java 17
Gestor backend: Maven
Frontend: Angular / web-angular dentro de monorepo
Gestor frontend: npm
Base de datos principal: PostgreSQL / Supabase
Migraciones: Flyway
Autenticacion: JWT stateless + Firebase token existente
Multi-tenant: si, tenant_id, TenantContextResolver, X-Tenant-Code
Facturacion: modulo SRI/RIDE existente
Correos: Resend + SMTP fallback
Vision AI: endpoints para YOLO, estadisticas y reportes
Tests recientes: 100 tests, 0 failures, 0 errors, BUILD SUCCESS
```

Regla:

```text
No rehacer arquitectura.
No romper Vision AI.
No romper SRI/RIDE.
No romper Resend.
No romper multi-tenant.
No eliminar Firebase sin revisar dependencias reales.
```

---

## 3. Problema actual que se debe resolver

El proyecto ya tiene modulos fuertes, pero antes de deploy serio necesita:

```text
1. Limpieza general de archivos.
2. Variables de entorno bien organizadas.
3. Secretos fuera del repositorio.
4. .gitignore fuerte.
5. Dockerfiles separados para backend y frontend.
6. Docker Compose local estable.
7. Perfil de produccion claro.
8. CORS configurable.
9. Login bien definido.
10. Rutas privadas protegidas.
11. Landing publica para vender/contactar.
12. Flujo comercial separado del sistema interno.
13. Documentacion de deploy.
14. Scripts de ejecucion.
15. Auditoria final.
```

---

## 4. Decision sobre login: Firebase, Google, Microsoft y acceso privado

### 4.1 Punto clave

El sistema no debe permitir que cualquier persona entre solo por dar clic en:

```text
Sign in with Google
Sign in with Microsoft
```

Si se permite OAuth abierto, cualquier usuario con Google/Microsoft podria crear cuenta y entrar, lo cual es incorrecto para un ERP multi-tenant privado.

### 4.2 Decision recomendada

Mantener el sistema como:

```text
Login privado por invitacion / usuario creado / usuario autorizado.
```

El acceso al sistema debe funcionar asi:

```text
1. Un negocio compra o solicita demo.
2. Un admin crea el tenant.
3. Un admin crea usuarios o envia invitaciones.
4. Solo usuarios autorizados pueden entrar.
5. Si alguien intenta entrar con Google/Microsoft y no esta autorizado, se rechaza.
```

### 4.3 Firebase: quitar o mantener

No eliminar Firebase automaticamente.

Codex debe revisar:

```text
- Si Firebase se usa realmente para auth.
- Si Firebase se usa para push notifications.
- Si Firebase se usa en frontend.
- Si FirebaseTokenFilter es requerido.
- Si existe login con Google/Microsoft ya implementado.
- Si existen variables Firebase en environment.
- Si alguna prueba depende de Firebase.
```

Decidir:

```text
Si Firebase solo esta parcialmente conectado y no se usa: dejarlo desactivable por variable.
Si Firebase se usa para login social: protegerlo con allowlist/invitacion.
Si Firebase se usa para notificaciones push: mantener solo push, no auth abierta.
```

### 4.4 Login social correcto si se usa Google/Microsoft

Google/Microsoft pueden existir, pero con esta regla:

```text
OAuth autentica identidad, pero NO autoriza acceso automaticamente.
```

Flujo correcto:

```text
Usuario da clic en Google/Microsoft
        ↓
Sistema valida identidad
        ↓
Backend revisa si ese email existe en users o invitations
        ↓
Si existe y esta activo: entra
        ↓
Si no existe: mostrar pantalla Acceso no autorizado / Solicitar demo
```

Nunca hacer:

```text
Usuario Google nuevo → crear cuenta automaticamente → entrar al ERP
```

Eso queda prohibido salvo que sea una landing publica o flujo comercial separado.

### 4.5 Registro publico

No debe existir registro publico directo al ERP.

Correcto:

```text
Landing publica → formulario de contacto/demo → equipo comercial crea tenant o agenda demo.
```

Incorrecto:

```text
Crear cuenta gratis → entrar al ERP completo sin aprobacion.
```

---

## 5. Nuevo flujo publico comercial

Se debe crear o revisar un apartado publico separado del sistema interno.

### 5.1 Rutas publicas

Frontend debe tener rutas publicas como:

```text
/
/precios
/demo
/contacto
/funcionalidades
/vision-ai
/facturacion-electronica
```

### 5.2 Rutas privadas

Rutas privadas:

```text
/app
/dashboard
/inventario
/facturacion
/reportes
/vision
/configuracion
/usuarios
```

### 5.3 Regla de seguridad frontend

Las rutas privadas deben tener guard:

```text
AuthGuard
TenantGuard
RoleGuard si existe
```

### 5.4 Regla de seguridad backend

El frontend guard no basta. El backend debe proteger:

```text
- endpoints con JWT
- tenant_id desde contexto autenticado
- roles/permisos
- X-Tenant-Code validado
```

---

## 6. Flujo comercial recomendado

### 6.1 Usuario interesado

```text
1. Entra a landing publica.
2. Ve funcionalidades.
3. Ve planes o CTA.
4. Da clic en Solicitar demo / Contactar ventas.
5. Llena formulario.
6. Sistema guarda lead.
7. Sistema envia correo a soporte@insightvisionia.cloud.
8. Equipo comercial contacta.
9. Si compra, se crea tenant.
10. Se crean usuarios autorizados.
```

### 6.2 Formulario de contacto

Campos:

```text
- nombre
- empresa
- correo
- telefono
- ciudad
- tipo de negocio
- numero de sucursales
- interes principal: ERP / Vision AI / Facturacion / Inventario / Todo
- mensaje
```

### 6.3 Tabla sugerida para leads

```sql
CREATE TABLE IF NOT EXISTS public_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(180) NOT NULL,
    company_name VARCHAR(180) NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    city VARCHAR(120) NULL,
    business_type VARCHAR(120) NULL,
    branch_count INTEGER NULL,
    interest VARCHAR(120) NULL,
    message TEXT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    source VARCHAR(80) NOT NULL DEFAULT 'LANDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_public_leads_status_created
ON public_leads (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_public_leads_email
ON public_leads (email);
```

### 6.4 Endpoint publico para leads

```http
POST /public/leads
```

Body:

```json
{
  "fullName": "David Mendez",
  "companyName": "Negocio Demo",
  "email": "cliente@correo.com",
  "phone": "0999999999",
  "city": "Cuenca",
  "businessType": "Retail",
  "branchCount": 2,
  "interest": "Vision AI + Facturacion",
  "message": "Quiero una demo"
}
```

Reglas:

```text
- Endpoint publico con rate limit.
- Validar email.
- No crear usuario del ERP.
- No crear tenant automaticamente.
- Guardar lead.
- Enviar notificacion interna a soporte@insightvisionia.cloud si Resend esta activo.
```

---

## 7. Limpieza general del proyecto

Codex debe revisar y limpiar:

```text
- Archivos temporales.
- Logs locales.
- Builds generados.
- target/
- dist/
- node_modules/
- .angular/cache/
- .idea si se decide no versionarla.
- .vscode si contiene configuraciones personales.
- .env reales.
- backups .bak, .old, .tmp.
- archivos con secretos.
- dumps de base de datos.
- capturas innecesarias.
- credenciales pegadas en codigo.
```

No eliminar:

```text
- migraciones Flyway.
- archivos fuente.
- archivos de configuracion ejemplo.
- README.
- Dockerfile.
- docker-compose.
- scripts utiles.
```

---

## 8. .gitignore recomendado

Crear o corregir `.gitignore` raiz:

```gitignore
# OS
.DS_Store
Thumbs.db

# IDE
.idea/
.vscode/
*.iml

# Logs
*.log
logs/

# Environment
.env
.env.*
!.env.example
!.env.template

# Java / Maven
apps/api/target/
apps/api/.mvn/wrapper/maven-wrapper.jar
*.class

# Node / Angular
node_modules/
apps/web-angular/node_modules/
apps/web-angular/dist/
apps/web-angular/.angular/cache/
.npm/

# Build outputs
dist/
build/
out/
coverage/

# Docker local volumes
.docker-data/
docker-data/
postgres-data/

# Uploads / generated files
uploads/
storage/
rides/
tmp/
temp/

# Database dumps
*.sql
*.dump
*.backup
*.bak

# Secrets / credentials
*.pem
*.key
*.p12
*.jks
firebase-service-account*.json
service-account*.json
credentials*.json

# Local tool files
.bruno/.env
bruno.env

# Misc
*.tmp
*.old
*.orig
```

Si el proyecto necesita versionar `.vscode` o `.idea`, Codex debe conservar solo archivos seguros y nunca secrets.

---

## 9. Variables de entorno

### 9.1 Regla general

Nunca subir secretos reales a Git.

Solo subir:

```text
.env.example
application-example.yml
README_ENV.md
```

No subir:

```text
.env
.env.prod
.env.local
application-prod.yml con secretos reales
firebase-service-account.json
```

### 9.2 Variables backend obligatorias

Crear `.env.example` backend con:

```properties
# Server
PORT=8080
SPRING_PROFILES_ACTIVE=prod
APP_PUBLIC_URL=http://localhost:8080
FRONTEND_URL=http://localhost:4200

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/insightvision
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# Security
JWT_SECRET=change_me_super_long_secret_min_64_chars
JWT_EXPIRATION_MINUTES=1440
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000

# Tenant
DEFAULT_TENANT_CODE=demo
ALLOW_TENANT_HEADER=true

# Resend
RESEND_ENABLED=true
RESEND_API_KEY=re_change_me
RESEND_BASE_URL=https://api.resend.com
RESEND_FROM_EMAIL=soporte@insightvisionia.cloud
RESEND_FROM_NAME=Insight Vision IA
RESEND_REPLY_TO=soporte@insightvisionia.cloud
RESEND_WEBHOOK_SECRET=whsec_change_me
RESEND_CONNECT_TIMEOUT_MS=5000
RESEND_READ_TIMEOUT_MS=15000
RESEND_MAX_ATTACHMENT_BYTES=10000000
RESEND_MAX_TOTAL_ATTACHMENT_BYTES=20000000

# Vision AI
VISION_RATE_LIMIT_PER_MINUTE=1800
VISION_API_KEY_PEPPER=change_me_vision_pepper

# Firebase
FIREBASE_ENABLED=false
FIREBASE_AUTH_ENABLED=false
FIREBASE_PUSH_ENABLED=false
FIREBASE_PROJECT_ID=
FIREBASE_CREDENTIALS_PATH=

# Storage
STORAGE_PROVIDER=local
STORAGE_LOCAL_PATH=./storage
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
SUPABASE_BUCKET=

# Logging
LOG_LEVEL=INFO
```

### 9.3 Variables frontend obligatorias

Crear `apps/web-angular/.env.example` o archivo equivalente segun sistema Angular:

```properties
API_URL=http://localhost:8080
PUBLIC_APP_NAME=Insight Vision IA
PUBLIC_SUPPORT_EMAIL=soporte@insightvisionia.cloud
PUBLIC_ENABLE_GOOGLE_LOGIN=false
PUBLIC_ENABLE_MICROSOFT_LOGIN=false
```

Codex debe verificar como Angular esta leyendo variables.

Si Angular no lee `.env` directamente, ajustar:

```text
- environment.ts
- environment.prod.ts
- replacement de Angular
- script de build que inyecte variables
```

---

## 10. Docker objetivo

El proyecto debe quedar con:

```text
Dockerfile backend
Dockerfile frontend
.dockerignore backend/frontend
compose.yaml local
compose.prod.yaml opcional
README_DOCKER.md
```

Estructura sugerida:

```text
/
  compose.yaml
  compose.prod.yaml
  .env.example
  README_DOCKER.md
  apps/
    api/
      Dockerfile
      .dockerignore
    web-angular/
      Dockerfile
      nginx.conf
      .dockerignore
```

---

## 11. Dockerfile backend Spring Boot

Archivo:

```text
apps/api/Dockerfile
```

Contenido sugerido:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw || true

RUN ./mvnw -q -DskipTests dependency:go-offline || mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q clean package -DskipTests || mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Codex debe ajustar si el proyecto no tiene `mvnw`.

---

## 12. .dockerignore backend

Archivo:

```text
apps/api/.dockerignore
```

Contenido:

```dockerignore
target/
.git/
.gitignore
.idea/
.vscode/
*.log
.env
.env.*
!.env.example
uploads/
storage/
tmp/
```

---

## 13. Dockerfile frontend Angular

Archivo:

```text
apps/web-angular/Dockerfile
```

Contenido sugerido:

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Ajustar esta ruta segun el output real de Angular.
# Codex debe detectar dist/<nombre-proyecto>/browser o dist/<nombre-proyecto>.
COPY --from=build /app/dist/ /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Codex debe verificar el output real de `ng build`.

---

## 14. nginx.conf frontend

Archivo:

```text
apps/web-angular/nginx.conf
```

Contenido:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /assets/ {
        try_files $uri =404;
        access_log off;
        expires 7d;
    }
}
```

---

## 15. .dockerignore frontend

Archivo:

```text
apps/web-angular/.dockerignore
```

Contenido:

```dockerignore
node_modules/
dist/
.angular/cache/
.git/
.gitignore
.idea/
.vscode/
*.log
.env
.env.*
!.env.example
```

---

## 16. Docker Compose local

Archivo raiz:

```text
compose.yaml
```

Contenido sugerido:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: insightvision-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: insightvision
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d insightvision"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build:
      context: ./apps/api
      dockerfile: Dockerfile
    container_name: insightvision-api
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      PORT: 8080
      SPRING_PROFILES_ACTIVE: docker
      DATABASE_URL: jdbc:postgresql://postgres:5432/insightvision
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET:-change_me_super_long_secret_min_64_chars_change_me}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:4200,http://localhost:3000,http://localhost:8081}
      RESEND_ENABLED: ${RESEND_ENABLED:-false}
      RESEND_API_KEY: ${RESEND_API_KEY:-}
      RESEND_FROM_EMAIL: ${RESEND_FROM_EMAIL:-soporte@insightvisionia.cloud}
      RESEND_FROM_NAME: ${RESEND_FROM_NAME:-Insight Vision IA}
      RESEND_REPLY_TO: ${RESEND_REPLY_TO:-soporte@insightvisionia.cloud}
      RESEND_WEBHOOK_SECRET: ${RESEND_WEBHOOK_SECRET:-}
      FIREBASE_ENABLED: ${FIREBASE_ENABLED:-false}
      FIREBASE_AUTH_ENABLED: ${FIREBASE_AUTH_ENABLED:-false}
      FIREBASE_PUSH_ENABLED: ${FIREBASE_PUSH_ENABLED:-false}
      VISION_RATE_LIMIT_PER_MINUTE: ${VISION_RATE_LIMIT_PER_MINUTE:-1800}
    ports:
      - "8080:8080"
    volumes:
      - api_storage:/app/storage

  web:
    build:
      context: ./apps/web-angular
      dockerfile: Dockerfile
    container_name: insightvision-web
    restart: unless-stopped
    depends_on:
      - api
    ports:
      - "8081:80"

volumes:
  postgres_data:
  api_storage:
```

---

## 17. Perfil docker Spring Boot

Crear:

```text
apps/api/src/main/resources/application-docker.yml
```

Contenido sugerido:

```yaml
server:
  port: ${PORT:8080}

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8081}
  sri:
    ride:
      resend:
        enabled: ${RESEND_ENABLED:false}
        api-key: ${RESEND_API_KEY:}
        base-url: ${RESEND_BASE_URL:https://api.resend.com}
        from-email: ${RESEND_FROM_EMAIL:soporte@insightvisionia.cloud}
        from-name: ${RESEND_FROM_NAME:Insight Vision IA}
        reply-to: ${RESEND_REPLY_TO:soporte@insightvisionia.cloud}
        webhook-secret: ${RESEND_WEBHOOK_SECRET:}
```

Codex debe adaptar a las propiedades reales del proyecto.

---

## 18. Docker escalable: no dockerizar a cada momento manualmente

### 18.1 Principio

No hay que rehacer Docker cada vez. El Dockerfile queda estable. Cada cambio de codigo se actualiza con:

```bash
docker compose up -d --build
```

O para desarrollo:

```bash
docker compose watch
```

Si se usa Docker Compose Watch, crear:

```yaml
develop:
  watch:
    - action: rebuild
      path: ./apps/api/src
    - action: rebuild
      path: ./apps/web-angular/src
```

Codex debe agregarlo solo si la version de Docker Compose del equipo lo soporta.

### 18.2 Scripts recomendados

Crear en `package.json` raiz:

```json
{
  "scripts": {
    "docker:up": "docker compose up -d --build",
    "docker:down": "docker compose down",
    "docker:logs": "docker compose logs -f",
    "docker:api:logs": "docker compose logs -f api",
    "docker:web:logs": "docker compose logs -f web",
    "docker:db:logs": "docker compose logs -f postgres"
  }
}
```

Si no existe `package.json` raiz o no conviene tocarlo, crear `scripts/docker-up.sh` y equivalentes.

---

## 19. Deploy portable

El proyecto debe poder subirse a cualquier proveedor con Docker.

### 19.1 Backend

Variables minimas para proveedor:

```text
PORT
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
RESEND_ENABLED
RESEND_API_KEY
RESEND_FROM_EMAIL
RESEND_FROM_NAME
RESEND_REPLY_TO
RESEND_WEBHOOK_SECRET
VISION_RATE_LIMIT_PER_MINUTE
FIREBASE_ENABLED
FIREBASE_AUTH_ENABLED
FIREBASE_PUSH_ENABLED
```

### 19.2 Frontend

Variables:

```text
API_URL
PUBLIC_APP_NAME
PUBLIC_SUPPORT_EMAIL
PUBLIC_ENABLE_GOOGLE_LOGIN
PUBLIC_ENABLE_MICROSOFT_LOGIN
```

### 19.3 CORS

Debe ser configurable:

```text
CORS_ALLOWED_ORIGINS=https://frontend-dominio.com,http://localhost:4200
```

Nunca dejar en produccion:

```text
*
```

---

## 20. Seguridad de variables

Codex debe revisar:

```text
- No hay API keys en commits.
- No hay claves Resend en application.yml real.
- No hay JWT_SECRET real en Git.
- No hay credenciales Firebase reales en Git.
- No hay claves Supabase service_role en frontend.
- No hay passwords de DB reales en Git.
- No hay tokens en Bruno exportados.
```

Si encuentra secretos reales:

```text
1. Remover del archivo.
2. Mover a .env local.
3. Agregar variable a .env.example sin valor real.
4. Recomendar rotar secreto si ya fue commiteado.
```

---

## 21. Login: tareas especificas para Codex

Codex debe inspeccionar:

```text
- SecurityConfig
- JwtAuthenticationFilter
- FirebaseTokenFilter
- LoginController/AuthController
- UserEntity
- Role/Permission
- frontend login component
- guards Angular
- rutas publicas/privadas
- configuracion Firebase
```

### 21.1 Reglas a implementar

```text
1. Login con email/password debe seguir funcionando si ya existe.
2. Google/Microsoft no deben crear usuarios automaticamente con acceso al ERP.
3. Si se mantiene Firebase Auth, validar email contra usuario existente o invitacion activa.
4. Si el usuario no existe, mostrar mensaje: Acceso no autorizado. Solicita una demo o contacta a soporte.
5. Crear CTA hacia /demo o /contacto.
6. Si Firebase no se usa, dejarlo desactivado por FIREBASE_AUTH_ENABLED=false.
7. Mantener Firebase Push si se usa para notificaciones, pero separado de Auth.
```

### 21.2 Pantallas frontend

Login debe mostrar:

```text
- Email/password.
- Google/Microsoft solo si estan habilitados.
- Link: Solicitar demo.
- Link: Contactar ventas.
```

Si login social no autorizado:

```text
Tu correo no tiene acceso a ningun negocio activo. Solicita una demo o contacta a soporte.
```

### 21.3 Backend authorization

Endpoint social login debe hacer:

```text
1. Validar token Google/Microsoft/Firebase.
2. Obtener email verificado.
3. Buscar usuario por email.
4. Verificar active=true.
5. Verificar tenant activo.
6. Generar JWT interno.
7. Si no existe, rechazar sin crear usuario ERP.
```

---

## 22. Multi-tenant y compra del sistema

El sistema debe diferenciar:

```text
Lead publico != Usuario ERP
```

### 22.1 Lead publico

Persona interesada que quiere comprar.

Puede llenar formulario.

No puede entrar al sistema.

### 22.2 Usuario ERP

Persona autorizada por un tenant.

Puede entrar si:

```text
- existe en users
- esta activo
- tiene tenant activo
- tiene rol/permisos
```

### 22.3 Tenant

Se crea solo por admin interno o proceso comercial.

No por registro publico abierto.

---

## 23. README requerido

Crear o actualizar:

```text
README.md
README_DOCKER.md
README_ENV.md
README_DEPLOY.md
```

### 23.1 README_DOCKER.md debe incluir

```markdown
# Docker local

## Requisitos
- Docker
- Docker Compose

## Levantar proyecto

```bash
docker compose up -d --build
```

## Ver logs

```bash
docker compose logs -f api
docker compose logs -f web
```

## URLs

- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432

## Bajar servicios

```bash
docker compose down
```

## Borrar volumen de base de datos local

```bash
docker compose down -v
```
```

### 23.2 README_ENV.md debe incluir

```text
- Explicacion de cada variable.
- Cuales son obligatorias.
- Cuales son solo produccion.
- Cuales son solo desarrollo.
- Como configurar Resend.
- Como configurar Firebase si se usa.
- Como configurar CORS.
```

### 23.3 README_DEPLOY.md debe incluir

```text
- Deploy con Docker.
- Deploy backend separado.
- Deploy frontend separado.
- Variables necesarias.
- Verificacion post-deploy.
- Probar /health.
- Probar login.
- Probar Vision AI.
- Probar Resend.
```

---

## 24. Pruebas obligatorias

Codex debe ejecutar:

```bash
cd apps/api
mvn -q test
```

Para frontend:

```bash
cd apps/web-angular
npm install
npm run build
```

Docker:

```bash
docker compose build
docker compose up -d
```

Verificar:

```text
- Backend levanta.
- Frontend carga.
- Flyway corre.
- Login funciona.
- Rutas privadas se protegen.
- Landing publica carga sin login.
- /public/leads funciona con rate limit.
- /vision/events funciona con API key.
- Resend settings no expone secretos.
```

---

## 25. Prompt maestro para Codex con GPT-5.5

Copiar y pegar en Codex dentro de WebStorm / IntelliJ IDEA:

```text
Necesito dejar el proyecto completamente limpio, seguro, dockerizado y listo para deploy portable.

Contexto del proyecto:
- Backend Spring Boot 3.5.7, Java 17, Maven.
- Frontend Angular en apps/web-angular.
- Monorepo con apps/api y apps/web-angular.
- Base PostgreSQL/Supabase.
- Flyway.
- JWT stateless.
- FirebaseTokenFilter existe, pero se debe revisar si se usa realmente.
- Multi-tenant con tenant_id, TenantContextResolver y X-Tenant-Code.
- Vision AI ya implementado.
- Resend/facturacion ya implementado.
- Tests recientes del backend pasan.

Objetivo:
1. Limpiar proyecto.
2. Proteger variables.
3. Corregir .gitignore.
4. Crear Dockerfile backend.
5. Crear Dockerfile frontend.
6. Crear .dockerignore backend/frontend.
7. Crear compose.yaml local.
8. Crear application-docker.yml si falta.
9. Crear documentacion Docker/env/deploy.
10. Revisar login y Firebase.
11. Evitar registro abierto al ERP.
12. Permitir login solo a usuarios existentes/autorizados.
13. Crear o preparar landing publica con rutas comerciales.
14. Crear flujo publico de lead/contacto/demo, sin crear usuario ERP.
15. Mantener multi-tenant seguro.
16. Ejecutar tests/builds.

Reglas de seguridad:
- No subir secretos reales.
- No exponer RESEND_API_KEY.
- No exponer JWT_SECRET.
- No exponer Supabase service_role al frontend.
- No exponer Firebase service account.
- No permitir login social abierto que cree usuarios ERP automaticamente.
- Google/Microsoft/Firebase Auth solo autentican identidad; el backend debe autorizar contra usuario existente o invitacion activa.
- Si el usuario no existe, rechazar y mostrar CTA de solicitar demo/contacto.

Login:
- Revisa SecurityConfig, JwtAuthenticationFilter, FirebaseTokenFilter, Auth/Login controllers, UserEntity, guards Angular y login component.
- Si Firebase Auth no es necesario, dejarlo desactivado por FIREBASE_AUTH_ENABLED=false.
- Si Firebase se usa para push, mantener FIREBASE_PUSH_ENABLED separado.
- Si Google/Microsoft se usan, no crear usuario automaticamente con acceso ERP.
- Validar email contra users o invitations.
- Solo usuarios activos de tenants activos pueden entrar.

Landing publica:
- Debe existir una parte publica con:
  - /
  - /precios
  - /demo
  - /contacto
  - /funcionalidades
- Debe tener CTA para solicitar demo.
- Formulario no crea usuario ERP.
- Formulario guarda lead publico o envia correo a soporte@insightvisionia.cloud.

Docker:
- Crear apps/api/Dockerfile multi-stage Java 17.
- Crear apps/api/.dockerignore.
- Crear apps/web-angular/Dockerfile multi-stage Node + Nginx.
- Crear apps/web-angular/nginx.conf.
- Crear apps/web-angular/.dockerignore.
- Crear compose.yaml con postgres, api y web.
- API debe usar server.port=${PORT:8080}.
- CORS debe ser configurable con CORS_ALLOWED_ORIGINS.
- No hardcodear dominios.

Variables:
- Crear/actualizar .env.example sin secretos reales.
- Crear/actualizar README_ENV.md.
- Variables minimas: DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, RESEND_API_KEY, RESEND_FROM_EMAIL, RESEND_REPLY_TO, RESEND_WEBHOOK_SECRET, CORS_ALLOWED_ORIGINS, FIREBASE_ENABLED, FIREBASE_AUTH_ENABLED, FIREBASE_PUSH_ENABLED, VISION_RATE_LIMIT_PER_MINUTE.

.gitignore:
- Ignorar .env reales, target, dist, node_modules, logs, storage, uploads, dumps, credenciales, firebase service account, claves .pem/.key/.p12/.jks.
- Permitir .env.example.

Pruebas:
- Ejecutar cd apps/api && mvn -q test.
- Ejecutar cd apps/web-angular && npm run build.
- Ejecutar docker compose build.
- Si algo falla, corregir.

Resultado final requerido:
Devuelve resumen con:
- Archivos creados.
- Archivos modificados.
- Variables agregadas.
- Decision sobre Firebase/Auth.
- Como ejecutar con Docker.
- Como desplegar.
- Tests ejecutados.
- Riesgos pendientes.

No rediseñar Vision AI ni Resend si ya funcionan.
No romper endpoints existentes.
No romper multi-tenant.
No crear usuarios ERP desde registro publico.
```

---

## 26. Prompt para Opus 4.7 como auditor final

Usar despues de que Codex aplique cambios:

```text
Actua como auditor tecnico senior. Revisa los cambios de limpieza, variables, Docker, login, Firebase/Auth y flujo comercial publico.

Busca:
1. Secretos reales en el repositorio.
2. .gitignore incompleto.
3. Dockerfile inseguro o pesado.
4. compose.yaml con secretos hardcodeados.
5. Variables faltantes.
6. CORS inseguro en produccion.
7. Firebase Auth permitiendo usuarios no autorizados.
8. Google/Microsoft login creando usuarios ERP automaticamente.
9. Rutas privadas sin guard.
10. Endpoints backend sin JWT/tenant cuando deberian tenerlo.
11. Landing publica creando usuarios ERP por error.
12. Lead/contacto sin rate limit.
13. Supabase service_role expuesta al frontend.
14. Resend API key expuesta.
15. Riesgos de deploy.
16. Tests/build faltantes.

Devuelve:
- Veredicto.
- Hallazgos bloqueantes.
- Hallazgos no bloqueantes.
- Recomendacion antes de deploy.
```

---

## 27. Resultado esperado final

El proyecto debe quedar asi:

```text
Proyecto limpio
Variables protegidas
.gitignore correcto
Docker backend listo
Docker frontend listo
Docker Compose local listo
README Docker/env/deploy listo
Login privado corregido
Firebase controlado por variables
Google/Microsoft sin registro abierto
Landing publica comercial
Formulario demo/contacto
ERP privado solo para usuarios autorizados
Multi-tenant intacto
Vision AI intacto
Resend/SRI intacto
Tests en verde
Build frontend correcto
Deploy portable
```

---

## 28. Checklist final

```text
[ ] .env real no esta en Git.
[ ] .env.example existe.
[ ] .gitignore protege secretos.
[ ] Backend Dockerfile existe.
[ ] Frontend Dockerfile existe.
[ ] compose.yaml existe.
[ ] Backend levanta con Docker.
[ ] Frontend levanta con Docker.
[ ] Flyway corre en Docker.
[ ] Login email/password funciona.
[ ] Google/Microsoft no crean usuarios ERP automaticamente.
[ ] Firebase Auth se puede desactivar.
[ ] Firebase Push queda separado si se usa.
[ ] Usuario no autorizado no entra.
[ ] Landing publica funciona sin login.
[ ] Formulario de demo/contacto no crea usuario ERP.
[ ] Rutas privadas protegidas.
[ ] Backend valida JWT y tenant.
[ ] CORS configurable.
[ ] Resend no expone API key.
[ ] Supabase service_role no esta en frontend.
[ ] Vision AI sigue funcionando.
[ ] SRI/RIDE sigue funcionando.
[ ] Tests backend pasan.
[ ] Build frontend pasa.
[ ] Docker compose build pasa.
```

