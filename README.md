# UCACUE Bar - ERP y POS

Sistema de cafeteria organizado como monorepo con una raiz orientada al dominio y no a artefactos temporales.

## Layout de la raiz

```text
ucacue-bar-spring/
|-- apps/
|   |-- api/                       # Spring Boot ERP API
|   |-- web-angular/               # Angular pilot UI
|-- integrations/
|   `-- firebase-functions/        # Cloud Functions y utilidades Firebase
|-- platform/
|   `-- supabase/                  # SQL, seeds y scripts operativos
|-- docs/                          # Arquitectura, roadmap y guias operativas vigentes
|-- ops/                           # Operacion del stack actual (mediamtx, SRI, migrate)
|-- docker-compose.yml             # Stack piloto principal con Angular
|-- package.json                   # Scripts de orquestacion de la raiz
`-- .gitignore
```

## Modulos principales

- `apps/api`: backend Spring Boot con ERP, POS, analitica y servicios de negocio.
- `apps/web-angular`: frontend Angular usado por el stack piloto principal.
- `integrations/firebase-functions`: funciones serverless y soporte Firebase.
- `platform/supabase`: extensiones, esquema, seeds y piezas operativas de base de datos.
- `ops`: artefactos operativos realmente usados por el stack actual.

## Inicio rapido

1. Configurar backend local:

   ```bash
    cp apps/api/.env.example apps/api/.env
    ```

2. Levantar el stack Docker principal:

   ```bash
    docker compose up -d --build
    ```

3. O ejecutar en local sin Docker:

   ```bash
   npm run frontend:start
   npm run backend:start:local
   ```

4. Servicios esperados:

   - Frontend Angular: `http://localhost`
   - API Spring Boot: `http://localhost:8090/api`
   - MySQL: `localhost:3310`
   - Redis: `localhost:6380`

## Scripts utiles

```bash
# Angular pilot
npm run frontend:start
npm run frontend:build

# API
npm run backend:compile
npm run backend:test

# Firebase Functions
npm run functions:lint
```

## Stack disponible

- `docker-compose.yml`: stack principal con `apps/api` + `apps/web-angular`.

## Criterios de orden aplicados

- La raiz solo expone modulos de negocio, integraciones, plataforma, operaciones y documentacion.
- Logs, builds, `node_modules`, `target`, runtime de Codex y residuos locales quedan fuera de la raiz visible y fuera de git.
- Los nombres ambiguos (`backend`, `frontend`, `functions`, `db`) se reemplazaron por carpetas que indican responsabilidad real.

## Documentacion

- `docs/README.md`
- `docs/ROADMAP.md`
- `docs/erp-modernization-architecture.md`

## Notas

- Los `.env` quedan como configuracion local. Los ejemplos versionables viven en sus respectivos `*.env.example`.
