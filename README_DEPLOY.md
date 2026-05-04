# Deploy portable

## Backend con Docker

Configura variables en el proveedor:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
PUBLIC_TENANT_REGISTRATION_ENABLED=false
FIREBASE_ENABLED=false
FIREBASE_AUTH_ENABLED=false
FIREBASE_PUSH_ENABLED=false
VISION_RATE_LIMIT_PER_MINUTE=1800
RESEND_ENABLED
RESEND_API_KEY
RESEND_WEBHOOK_SECRET
```

## Frontend con Docker

Construye `apps/web-angular/Dockerfile` y configura `public/env.js` o reemplazo equivalente en el proveedor:

```text
apiBaseUrl=https://api.tu-dominio.com/api
publicEnableGoogleLogin=false
publicEnableMicrosoftLogin=false
```

## Verificacion post-deploy

1. Abrir landing publica.
2. Enviar `/public/leads`.
3. Confirmar que `/login` funciona solo con usuarios existentes.
4. Confirmar que Google/Microsoft no crean usuarios ERP nuevos.
5. Probar health del backend.
6. Probar `/vision/events` con API key valida.
7. Probar Resend webhook con timestamp valido.

## Reglas de produccion

- No usar CORS `*`.
- No poner `.env`, Firebase service account, Supabase service role, Resend API key ni certificados SRI en Git.
- Crear tenants y usuarios ERP solo por admin/proceso comercial autorizado.
