# UCACUE Bar Frontend (static)

Servido por Nginx en http://localhost:3001, con proxy a backend en http://localhost:8080/api.

## Requisitos
- Docker Desktop activo.

## Ejecutar
Desde la raíz del repo:

```
docker compose -f ops/docker-compose.yml up -d --build
```

Ver estado:
```
docker ps
docker logs ucacue_nginx --tail 100
docker logs ucacue_app --tail 100
```

Probar:
```
curl -I http://localhost:3001/pages/login.html
curl -I http://localhost:8080/api/actuator/health
```

Abrir:
- Frontend: http://localhost:3001/pages/login.html
- Dashboard: http://localhost:3001/pages/dashboard.html

## Credenciales por defecto
- admin: admin@ucacue.edu.ec / Admin123!
- comprador: comprador@ucacue.edu.ec / Admin123!

## Estructura
- /pages/*.html (login, dashboard, pos)
- /js/*.js (theme, api, guard, dashboard, pos)
- /css/app.css (estilos auxiliares)
- /assets/* (iconos, logos, etc.)

## CDNs usadas
- Tailwind: https://cdn.tailwindcss.com
- Flowbite: https://cdn.jsdelivr.net/npm/flowbite@2.5.1/dist/flowbite.min.css y .js
- GSAP: https://cdn.jsdelivr.net/npm/gsap@3.12.5/dist/gsap.min.js
- Tabler Icons: https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@latest/tabler-icons.min.css

## Notas
- Base URL API siempre es `/api` (Nginx hace proxy a backend).
- Modo oscuro: botón en el dashboard, persiste en localStorage.
- Guard: redirige a login si no hay token.
- Login/Registro manejan estados de carga y errores (toasts).
