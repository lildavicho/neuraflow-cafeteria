# Docker local

## Requisitos

- Docker
- Docker Compose

## Levantar proyecto

```bash
npm run docker:up
```

## URLs

- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- Health backend: http://localhost:8080/api/actuator/health
- PostgreSQL: localhost:5432

## Logs

```bash
npm run docker:api
npm run docker:web
npm run docker:logs
```

## Bajar servicios

```bash
npm run docker:down
```

## Borrar volumen local

```bash
docker compose down -v
```

## Notas

- El compose local usa PostgreSQL 16 y el perfil Spring `docker`.
- El servicio `web` usa el frontend Angular/TypeScript de `apps/web-angular`.
- Nginx sirve la SPA con `try_files $uri $uri/ /index.html`.
- No uses `Clear-Site-Data` como header permanente. Para limpiar cache en desarrollo, usa DevTools > Application > Clear storage.
- Para reconstruir solo frontend: `npm run docker:build:web`.
- Para reconstruir solo backend: `npm run docker:build:api`.
- Para verificar archivos dentro del frontend: `docker exec -it insightvision-web sh`, luego `ls -la /usr/share/nginx/html`, `ls -la /usr/share/nginx/html/src` si aplica, y `cat /etc/nginx/conf.d/default.conf`.
- El registro publico al ERP queda desactivado por defecto.
- El formulario publico guarda leads en `public_leads` y no crea tenants ni usuarios.
