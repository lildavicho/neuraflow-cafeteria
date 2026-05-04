# Variables de entorno

Nunca subas secretos reales al repositorio. Usa `.env.example` y `apps/api/.env.example` como plantilla.

## Backend obligatorias

- `DATABASE_URL` o `DB_URL`: JDBC PostgreSQL.
- `DATABASE_USERNAME`/`DATABASE_PASSWORD` o `DB_USER`/`DB_PASS`: credenciales de base.
- `JWT_SECRET`: secreto largo, minimo 32 bytes.
- `CORS_ALLOWED_ORIGINS`: dominios permitidos, sin `*` en produccion.

## Seguridad y acceso

- `PUBLIC_TENANT_REGISTRATION_ENABLED=false`: mantiene cerrado `/auth/register-tenant`.
- `FIREBASE_ENABLED=false`: apaga Firebase Admin.
- `FIREBASE_AUTH_ENABLED=false`: apaga login social Firebase.
- `FIREBASE_PUSH_ENABLED=false`: apaga push FCM.
- `VISION_RATE_LIMIT_PER_MINUTE=1800`: limite de ingestion Vision por API key.

## Resend y SRI

- `RESEND_ENABLED`: habilita envio por Resend.
- `RESEND_API_KEY`: secreto, nunca en frontend.
- `RESEND_WEBHOOK_SECRET`: secreto Svix/Resend.
- `SUPABASE_SERVICE_ROLE_KEY`: secreto backend para storage; nunca en Angular.
- `PUBLIC_LEADS_NOTIFY_TO`: correo interno para leads comerciales.

## Frontend runtime

El frontend lee `public/env.js` en runtime:

- `apiBaseUrl`
- `publicAppName`
- `publicSupportEmail`
- `publicEnableGoogleLogin`
- `publicEnableMicrosoftLogin`
- `firebase` solo si se habilita login social.

Los valores reales de Firebase Web no son secretos de servidor, pero no deben activar acceso al ERP sin autorizacion backend.
