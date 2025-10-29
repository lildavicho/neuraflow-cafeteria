# UCACUE Bar – Auditoría integral y correcciones

Fecha: 2025-10-28
Repositorio: c:\Sistema Bar Final\ucacue-bar-spring

## Resumen ejecutivo
- Se normalizó la configuración de Firebase en el frontend (.env y firebase.js) y se habilitó persistencia local.
- Se corrigió CORS/CSP del backend para permitir el puerto 5174 (Vite dev) además de 5173 (Nginx).
- Se mejoró authService.js (email/contraseña como foco; Google opcional con client_id) y se reparó un fallo de compilación al añadir MicrosoftAuthProvider tras agregar loginWithMicrosoft.
- Se fortaleció .gitignore para evitar subir secretos (envs y credenciales de Firebase) y artefactos de build.
- No se encontraron importaciones CDN de Firebase en el frontend; todo usa SDK modular (NPM).
- Se prepararon comandos y plan de validación. Se incluyen parches y diffs en .audit/.

## Inventario y stack
- frontend/ (React 19 + Vite 5 + Tailwind, ESLint, Firebase SDK modular)
  - scripts: dev, build, lint, preview
- backend/ (Java 17, Spring Boot 3.3, JPA, Security, Swagger, Algolia, Firebase Admin)
  - build: Maven (spring-boot-maven-plugin)
- functions/ (Firebase Functions Node)
- ops/ (docker-compose, nginx, mediamtx)

## Cambios aplicados (resumen)
1) frontend/.env: claves exactas de Firebase (baru-fe8a3), bucket appspot.com, measurementId y API Base URL.
2) frontend/src/services/firebase.js: sólo variables de entorno; measurementId; persistencia local (browserLocalPersistence); inicialización de Messaging protegida por isSupported().
3) frontend/src/services/authService.js: Google provider con prompt select_account + client_id; import MicrosoftAuthProvider para el flujo agregado; textos de error en español.
4) backend/src/.../SecurityConfig.java: se agregan 5174/127.0.0.1:5174 a CORS y CSP connect-src.
5) .gitignore: ignorar .env, credenciales Firebase y dist del frontend.

Los diffs completos se adjuntan en .audit/diffs/.

## Análisis estático y seguridad (SAST)
- Búsqueda de patrones peligrosos: eval/exec → no hallado.
- Secretos en repo:
  - .env raíz contiene JWT_SECRET (crítico). Se agregó ignore para .env a nivel repo. Recomendación: rotar el secreto si ya fue expuesto.
  - serviceAccount.json (ruta referenciada). Se agregó patrón para ignorarlo.
- CDN Firebase: no se usa (OK). Google Fonts vía gstatic es aceptable.

## CORS/CSP
- Backend permitía 5173 pero no 5174; se añadió 5174 a CORS y CSP connect-src.
- Recomendación: parametrizar orígenes por perfiles para dev/prod.

## Pruebas propuestas
Frontend (dev):
- Comandos:
  - npm i (en frontend/)
  - npm run dev → http://localhost:5174/login
- Casos:
  - Registro email/contraseña → redirección a /dashboard.
  - Login email/contraseña (incl. admin@ucacue.edu.ec / Admin123!) → /dashboard.
  - Persistencia: cerrar/abrir navegador mantiene sesión.

Frontend (prod con Nginx):
- npm run build (frontend/)
- docker compose -f ops/docker-compose.yml up -d nginx
- Abrir http://localhost:5173/login

Backend:
- mvn -B -DskipTests package (backend/)
- mvn test (si hay tests) 

Funciones Firebase (si se usan):
- npm i (functions/)
- firebase emulators:start (si aplica)

Nota: Estos comandos no se ejecutaron automáticamente en este entorno por políticas; ejecutar localmente y adjuntar logs en .audit/logs si se desea.

## Problemas identificados y severidad
- frontend/.env con valores inconsistentes y comillas (Alta) → Corregido.
- firebase.js con fallbacks en runtime (Media) → Corregido a env-only con measurementId.
- CSP/CORS sin 5174 (Alta) → Corregido.
- authService: falta client_id Google y (tras cambio del usuario) falta import MicrosoftAuthProvider (Alta para build) → Corregido.
- Riesgo de exponer secretos (.env, service accounts) (Crítica) → .gitignore endurecido; se recomienda rotación de secretos si ya se publicaron.

## Recomendaciones
- Agregar CI (GitHub Actions) con jobs de lint/build para frontend y backend.
- Añadir Prettier en frontend (consistente con ESLint) y Checkstyle/Spotless en backend.
- Tests mínimos de autenticación (mock Firebase) y de controladores en backend.
- Centralizar configuración de CORS por perfiles y externalizar orígenes permitidos.

## Propuesta de rama y commits
- Rama sugerida: audit/fix-20251028-2215
- Commits atómicos (convencionales):
  - fix(frontend): normalizar .env con claves Firebase y measurementId
  - fix(frontend): reescribir firebase.js (env-only, persistence, messaging)
  - fix(auth): client_id Google + import MicrosoftAuthProvider
  - fix(backend): CORS/CSP añadir 5174
  - chore(gitignore): ignorar .env, credenciales y dist

¿Autorizar creación de rama y commits automáticos? Si es afirmativo, generaré la rama, haré los commits y opcionalmente abriré PR con checklist.

## Apéndice: Validación de dominios autorizados en Firebase Auth
- Agregar: http://localhost:5174, http://localhost:5173, baru-fe8a3.firebaseapp.com, baru-fe8a3.web.app
- Proveedores: Email/Contraseña (obligatorio), Google (opcional)

