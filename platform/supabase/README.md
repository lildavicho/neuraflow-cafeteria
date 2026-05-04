# Supabase PostgreSQL

La ruta de migraciones valida para Supabase es:

`apps/api/src/main/resources/db/migration/postgresql`

Tambien se mantiene una copia operativa en:

`platform/supabase/migrations`

El backend usa Flyway para aplicar estas migraciones en el esquema `bar_app`.
La carpeta anterior tenia SQL de bootstrap separado del runtime Spring; ya no debe usarse.

Para ejecutar manualmente desde Supabase SQL Editor, respeta el orden `V1`, `V2`, `V3`.
