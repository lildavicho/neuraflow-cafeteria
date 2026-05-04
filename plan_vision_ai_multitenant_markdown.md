# PLAN MAESTRO EN MARKDOWN
# Modulo Vision AI multi-tenant para camaras, YOLO, estadisticas, dashboards y reportes

## 0. Decision de modelos

### Modelo principal para ejecutar en WebStorm / IntelliJ IDEA

**Usar Codex con GPT-5.5 como modelo principal de implementacion.**

Motivo operativo:

- Debe leer el repositorio real.
- Debe modificar archivos existentes.
- Debe crear migraciones.
- Debe correr pruebas.
- Debe respetar la arquitectura actual.
- Debe conectar el modulo Vision AI con dashboards, reportes, tenants, usuarios, camaras y estadisticas.

### Modelo para planificar y ordenar antes de tocar codigo

**Usar GPT-5.5 en ChatGPT.**

Uso:

- Definir arquitectura.
- Revisar flujo general.
- Crear contratos JSON.
- Revisar si el modulo esta alineado con el negocio.
- Validar que el endpoint sea consumible por el sistema YOLO del companero.

### Modelo para auditoria final

**Usar Opus 4.7 solo como revisor externo.**

Uso:

- Revisar aislamiento multi-tenant.
- Revisar posibles fugas de datos.
- Revisar errores de concurrencia.
- Revisar endpoints.
- Revisar si las estadisticas pueden mezclarse entre negocios.
- Revisar rendimiento.

### Orden recomendado

```text
1. GPT-5.5: revisar este plan y ajustarlo al contexto del proyecto.
2. Codex con GPT-5.5: implementar migraciones, backend, servicios, endpoints y pruebas.
3. GPT-5.5: revisar documentacion y contrato para el companero de YOLO.
4. Opus 4.7: auditoria externa de seguridad, multi-tenancy y rendimiento.
5. Codex con GPT-5.5: aplicar correcciones finales.
```

---

## 1. Objetivo general

Implementar un modulo **Vision AI multi-tenant** preparado para muchos negocios funcionando en simultaneo.

El modulo debe recibir eventos de deteccion provenientes del sistema YOLO de un companero, validar que la camara pertenezca al negocio correcto, guardar eventos, guardar detecciones, calcular estadisticas y exponer endpoints para que los dashboards, reportes y demas modulos puedan consumir informacion sin mezclar datos entre tenants.

---

## 2. Problema que se debe resolver

Actualmente se quiere abrir un endpoint para que un sistema YOLO envie datos de deteccion. El riesgo principal es que si no existe separacion estricta por tenant, las estadisticas de un negocio pueden mezclarse con las de otro.

Ejemplo de error grave:

```sql
SELECT *
FROM vision_events
WHERE camera_id = 'CAM-001';
```

Ese query es incorrecto porque varios negocios pueden tener una camara con codigo `CAM-001`.

Forma correcta:

```sql
SELECT *
FROM vision_events
WHERE tenant_id = :tenantId
AND camera_id = :cameraId;
```

Regla central:

```text
Ninguna consulta del modulo Vision AI puede ejecutarse sin tenant_id.
```

---

## 3. Datos no verificados que Codex debe inspeccionar primero

Antes de modificar archivos, Codex debe revisar el proyecto y confirmar:

```text
- Framework backend real.
- Motor de base de datos real.
- Sistema de migraciones usado: Flyway, Liquibase, Prisma, TypeORM, Sequelize, Knex, scripts SQL manuales u otro.
- Existencia de tabla tenants.
- Existencia de tabla users.
- Existencia de tabla roles/permisos.
- Existencia de tabla branches/sucursales/locales.
- Existencia de tabla cameras o dispositivos.
- Existencia de dashboards/reportes.
- Sistema de autenticacion actual: JWT, session, API Key, OAuth, Supabase Auth u otro.
- Estructura de carpetas del proyecto.
```

Si algo ya existe, Codex no debe duplicarlo. Debe adaptar las migraciones y el codigo a los nombres reales.

---

## 4. Principios obligatorios de arquitectura

### 4.1 Separacion multi-tenant estricta

Toda informacion relacionada al modulo debe estar ligada a `tenant_id`.

Tablas que deben tener `tenant_id`:

```text
- vision_cameras
- vision_camera_locations
- vision_api_keys
- vision_events
- vision_detections
- vision_statistics_hourly
- vision_statistics_daily
- vision_statistics_monthly
- vision_alerts
- vision_processing_logs
- vision_webhook_logs
- vision_audit_logs
```

Si el sistema ya tiene tablas generales de camaras, sucursales o tenants, se deben reutilizar.

### 4.2 No confiar en tenantId enviado por el body

El `tenantId` no debe venir como dato confiable desde el request del sistema YOLO.

El backend debe resolver el tenant desde:

```text
Opcion A: API Key por tenant para Vision AI.
Opcion B: JWT del usuario/sistema.
Opcion C: Token interno firmado para servicios.
```

Opcion recomendada para el sistema YOLO del companero:

```text
API Key por tenant, enviada en header.
```

Header recomendado:

```http
X-Vision-Api-Key: vk_live_xxxxxxxxxxxxxxxxxxxxx
```

El backend debe buscar esa API Key en base de datos usando hash, resolver el `tenant_id`, verificar que este activa y luego permitir el ingreso del evento.

### 4.3 Validar camara contra tenant

Antes de guardar detecciones:

```text
1. Resolver tenant desde API Key o JWT.
2. Leer cameraCode/cameraId del request.
3. Buscar camara con tenant_id + cameraCode.
4. Confirmar que esta activa.
5. Confirmar que pertenece a ese tenant.
6. Si no pertenece, rechazar con 403 o 404 controlado.
```

### 4.4 Estadisticas siempre segmentadas

Toda estadistica debe calcularse por:

```text
- tenant_id
- camera_id
- location_id/sucursal_id
- label
- periodo
```

Nunca se debe agrupar globalmente sin tenant.

### 4.5 Produccion y escalabilidad

El sistema debe estar preparado para:

```text
- Muchos tenants.
- Muchas camaras por tenant.
- Multiples eventos por segundo.
- Consultas rapidas para dashboards.
- Reportes diarios, semanales y mensuales.
- Reintentos del sistema YOLO.
- Idempotencia para evitar eventos duplicados.
- Auditoria de accesos y errores.
```

---

## 5. Flujo funcional completo

```text
Sistema YOLO del companero
        ↓
Envia POST /api/vision/events
        ↓
Header: X-Vision-Api-Key
        ↓
Backend resuelve tenant_id desde API Key
        ↓
Backend valida que la camara pertenezca al tenant
        ↓
Backend valida payload
        ↓
Backend revisa idempotency_key o frame_id para evitar duplicados
        ↓
Backend guarda vision_event
        ↓
Backend guarda vision_detections
        ↓
Backend actualiza estadisticas agregadas
        ↓
Dashboard consume GET /api/vision/statistics
        ↓
Reportes consumen GET /api/vision/reports
```

---

## 6. Contrato del endpoint para YOLO

### 6.1 Endpoint principal

```http
POST /api/vision/events
```

### 6.2 Headers

```http
Content-Type: application/json
X-Vision-Api-Key: vk_live_xxxxxxxxxxxxxxxxxxxxx
Idempotency-Key: tenant-camera-frame-timestamp-opcional
```

### 6.3 Body recomendado

```json
{
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "timestamp": "2026-05-02T18:00:00-05:00",
  "frameId": "frame-000123",
  "source": "yolo-service",
  "model": {
    "name": "YOLO",
    "version": "v8-or-custom",
    "confidenceThreshold": 0.65
  },
  "image": {
    "width": 1920,
    "height": 1080,
    "uri": null
  },
  "detections": [
    {
      "label": "persona",
      "confidence": 0.94,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 220,
        "height": 410
      },
      "trackingId": "track-001"
    }
  ],
  "metadata": {
    "device": "vision-ai-camera",
    "fps": 15,
    "processingMs": 48
  }
}
```

### 6.4 Respuesta correcta

```json
{
  "success": true,
  "eventId": "uuid-event-id",
  "tenantResolved": true,
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "receivedDetections": 1,
  "storedDetections": 1,
  "statisticsUpdated": true,
  "message": "Vision event stored successfully"
}
```

### 6.5 Respuestas de error

#### API Key ausente

```json
{
  "success": false,
  "code": "VISION_API_KEY_REQUIRED",
  "message": "Vision API key is required"
}
```

#### API Key invalida

```json
{
  "success": false,
  "code": "VISION_API_KEY_INVALID",
  "message": "Invalid or inactive Vision API key"
}
```

#### Camara no pertenece al tenant

```json
{
  "success": false,
  "code": "CAMERA_NOT_FOUND_FOR_TENANT",
  "message": "Camera does not exist or is not active for this tenant"
}
```

#### Payload invalido

```json
{
  "success": false,
  "code": "VISION_PAYLOAD_INVALID",
  "message": "Invalid detection payload"
}
```

#### Evento duplicado

```json
{
  "success": true,
  "code": "VISION_EVENT_ALREADY_PROCESSED",
  "message": "Event was already processed",
  "eventId": "uuid-event-id"
}
```

---

## 7. Endpoints internos para dashboard y reportes

### 7.1 Estadisticas generales

```http
GET /api/vision/statistics?from=2026-05-01&to=2026-05-02&period=daily
```

El tenant debe venir desde JWT del usuario autenticado. No desde query param.

Respuesta:

```json
{
  "tenantId": "hidden-or-internal",
  "period": "daily",
  "from": "2026-05-01",
  "to": "2026-05-02",
  "summary": {
    "totalEvents": 1500,
    "totalDetections": 4380,
    "activeCameras": 8,
    "topLabel": "persona"
  },
  "byLabel": [
    {
      "label": "persona",
      "total": 3900
    },
    {
      "label": "vehiculo",
      "total": 480
    }
  ],
  "byCamera": [
    {
      "cameraCode": "CAM-001",
      "cameraName": "Entrada principal",
      "totalDetections": 1200
    }
  ],
  "timeline": [
    {
      "periodStart": "2026-05-01T00:00:00-05:00",
      "totalDetections": 800
    }
  ]
}
```

### 7.2 Estadisticas por camara

```http
GET /api/vision/cameras/{cameraCode}/statistics?from=2026-05-01&to=2026-05-02&period=hourly
```

Regla:

```text
El backend debe buscar la camara por tenant_id + cameraCode.
```

### 7.3 Eventos recientes

```http
GET /api/vision/events/recent?limit=50
```

Debe filtrar internamente por tenant.

### 7.4 Reporte para exportacion

```http
GET /api/vision/reports/detections?from=2026-05-01&to=2026-05-31&format=json
```

Opcional:

```http
GET /api/vision/reports/detections/export?from=2026-05-01&to=2026-05-31&format=csv
```

---

## 8. Modelo de datos recomendado

### 8.1 Entidades principales

```text
tenants
vision_api_keys
vision_locations
vision_cameras
vision_events
vision_detections
vision_statistics_hourly
vision_statistics_daily
vision_statistics_monthly
vision_processing_logs
vision_audit_logs
```

Si el sistema ya tiene `tenants`, `branches`, `locations`, `users`, no crear duplicados.

---

## 9. Migraciones SQL PostgreSQL

### 9.1 Archivo sugerido

Si usas Flyway:

```text
src/main/resources/db/migration/V2026050201__vision_ai_multitenant_core.sql
```

Si usas otro sistema, adaptar nombre.

---

## 10. Migracion 1: extensiones y tipos

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vision_api_key_status') THEN
        CREATE TYPE vision_api_key_status AS ENUM ('ACTIVE', 'INACTIVE', 'REVOKED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vision_camera_status') THEN
        CREATE TYPE vision_camera_status AS ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vision_event_status') THEN
        CREATE TYPE vision_event_status AS ENUM ('RECEIVED', 'PROCESSED', 'DUPLICATED', 'FAILED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vision_period_type') THEN
        CREATE TYPE vision_period_type AS ENUM ('HOURLY', 'DAILY', 'MONTHLY');
    END IF;
END $$;
```

---

## 11. Migracion 2: tabla de API Keys por tenant

> Si ya existe tabla general de API keys, adaptar esta tabla o crear una especial para Vision AI.

```sql
CREATE TABLE IF NOT EXISTS vision_api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    key_prefix VARCHAR(30) NOT NULL,
    key_hash TEXT NOT NULL,
    status vision_api_key_status NOT NULL DEFAULT 'ACTIVE',
    last_used_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ NULL,
    created_by UUID NULL,
    CONSTRAINT uk_vision_api_keys_hash UNIQUE (key_hash)
);

CREATE INDEX IF NOT EXISTS idx_vision_api_keys_tenant_status
ON vision_api_keys (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_vision_api_keys_prefix
ON vision_api_keys (key_prefix);
```

### Regla de seguridad

Nunca guardar la API Key completa en texto plano.

Guardar:

```text
key_prefix: primeros caracteres visibles
key_hash: hash seguro de la key completa
```

---

## 12. Migracion 3: ubicaciones o sucursales Vision

> Si ya existe tabla `locations`, `branches` o `sucursales`, no duplicar. En ese caso, usar FK hacia la tabla existente.

```sql
CREATE TABLE IF NOT EXISTS vision_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT NULL,
    address TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vision_locations_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_vision_locations_tenant
ON vision_locations (tenant_id);
```

---

## 13. Migracion 4: camaras Vision AI

```sql
CREATE TABLE IF NOT EXISTS vision_cameras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    location_id UUID NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT NULL,
    stream_url TEXT NULL,
    external_device_id VARCHAR(150) NULL,
    status vision_camera_status NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vision_cameras_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_vision_cameras_location
        FOREIGN KEY (location_id)
        REFERENCES vision_locations(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_vision_cameras_tenant_status
ON vision_cameras (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_vision_cameras_tenant_location
ON vision_cameras (tenant_id, location_id);

CREATE INDEX IF NOT EXISTS idx_vision_cameras_metadata_gin
ON vision_cameras USING GIN (metadata);
```

### Regla critica

`code` puede repetirse entre tenants, pero no dentro del mismo tenant.

Correcto:

```text
Tenant A -> CAM-001
Tenant B -> CAM-001
```

Incorrecto:

```text
Tenant A -> dos camaras CAM-001
```

---

## 14. Migracion 5: eventos Vision

```sql
CREATE TABLE IF NOT EXISTS vision_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    camera_id UUID NOT NULL,
    location_id UUID NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    frame_id VARCHAR(180) NULL,
    idempotency_key VARCHAR(250) NULL,
    source VARCHAR(120) NOT NULL DEFAULT 'yolo-service',
    model_name VARCHAR(120) NULL,
    model_version VARCHAR(120) NULL,
    confidence_threshold NUMERIC(5,4) NULL,
    image_width INTEGER NULL,
    image_height INTEGER NULL,
    image_uri TEXT NULL,
    total_detections INTEGER NOT NULL DEFAULT 0,
    status vision_event_status NOT NULL DEFAULT 'RECEIVED',
    processing_ms INTEGER NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vision_events_camera
        FOREIGN KEY (camera_id)
        REFERENCES vision_cameras(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_vision_events_location
        FOREIGN KEY (location_id)
        REFERENCES vision_locations(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_vision_events_total_detections
        CHECK (total_detections >= 0),
    CONSTRAINT chk_vision_events_image_width
        CHECK (image_width IS NULL OR image_width > 0),
    CONSTRAINT chk_vision_events_image_height
        CHECK (image_height IS NULL OR image_height > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_events_tenant_idempotency
ON vision_events (tenant_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_events_tenant_camera_frame
ON vision_events (tenant_id, camera_id, frame_id)
WHERE frame_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vision_events_tenant_camera_time
ON vision_events (tenant_id, camera_id, event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_vision_events_tenant_location_time
ON vision_events (tenant_id, location_id, event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_vision_events_tenant_time
ON vision_events (tenant_id, event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_vision_events_status
ON vision_events (status);

CREATE INDEX IF NOT EXISTS idx_vision_events_metadata_gin
ON vision_events USING GIN (metadata);
```

---

## 15. Migracion 6: detecciones

```sql
CREATE TABLE IF NOT EXISTS vision_detections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    camera_id UUID NOT NULL,
    location_id UUID NULL,
    label VARCHAR(120) NOT NULL,
    confidence NUMERIC(6,5) NOT NULL,
    bbox_x NUMERIC(12,4) NOT NULL,
    bbox_y NUMERIC(12,4) NOT NULL,
    bbox_width NUMERIC(12,4) NOT NULL,
    bbox_height NUMERIC(12,4) NOT NULL,
    tracking_id VARCHAR(180) NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vision_detections_event
        FOREIGN KEY (event_id)
        REFERENCES vision_events(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_vision_detections_camera
        FOREIGN KEY (camera_id)
        REFERENCES vision_cameras(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_vision_detections_location
        FOREIGN KEY (location_id)
        REFERENCES vision_locations(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_vision_detections_confidence
        CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT chk_vision_detections_bbox_width
        CHECK (bbox_width > 0),
    CONSTRAINT chk_vision_detections_bbox_height
        CHECK (bbox_height > 0)
);

CREATE INDEX IF NOT EXISTS idx_vision_detections_tenant_event
ON vision_detections (tenant_id, event_id);

CREATE INDEX IF NOT EXISTS idx_vision_detections_tenant_camera_label
ON vision_detections (tenant_id, camera_id, label);

CREATE INDEX IF NOT EXISTS idx_vision_detections_tenant_label_created
ON vision_detections (tenant_id, label, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vision_detections_tenant_location_label
ON vision_detections (tenant_id, location_id, label);

CREATE INDEX IF NOT EXISTS idx_vision_detections_metadata_gin
ON vision_detections USING GIN (metadata);
```

---

## 16. Migracion 7: estadisticas por hora

```sql
CREATE TABLE IF NOT EXISTS vision_statistics_hourly (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    camera_id UUID NULL,
    location_id UUID NULL,
    label VARCHAR(120) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    total_events INTEGER NOT NULL DEFAULT 0,
    total_detections INTEGER NOT NULL DEFAULT 0,
    avg_confidence NUMERIC(8,5) NULL,
    max_confidence NUMERIC(8,5) NULL,
    min_confidence NUMERIC(8,5) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vision_stats_hourly_scope UNIQUE (tenant_id, camera_id, location_id, label, period_start),
    CONSTRAINT chk_vision_stats_hourly_total_events CHECK (total_events >= 0),
    CONSTRAINT chk_vision_stats_hourly_total_detections CHECK (total_detections >= 0)
);

CREATE INDEX IF NOT EXISTS idx_vision_stats_hourly_tenant_period
ON vision_statistics_hourly (tenant_id, period_start DESC, period_end DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_hourly_tenant_camera_period
ON vision_statistics_hourly (tenant_id, camera_id, period_start DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_hourly_tenant_location_period
ON vision_statistics_hourly (tenant_id, location_id, period_start DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_hourly_tenant_label_period
ON vision_statistics_hourly (tenant_id, label, period_start DESC);
```

---

## 17. Migracion 8: estadisticas diarias

```sql
CREATE TABLE IF NOT EXISTS vision_statistics_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    camera_id UUID NULL,
    location_id UUID NULL,
    label VARCHAR(120) NOT NULL,
    period_date DATE NOT NULL,
    total_events INTEGER NOT NULL DEFAULT 0,
    total_detections INTEGER NOT NULL DEFAULT 0,
    avg_confidence NUMERIC(8,5) NULL,
    max_confidence NUMERIC(8,5) NULL,
    min_confidence NUMERIC(8,5) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vision_stats_daily_scope UNIQUE (tenant_id, camera_id, location_id, label, period_date),
    CONSTRAINT chk_vision_stats_daily_total_events CHECK (total_events >= 0),
    CONSTRAINT chk_vision_stats_daily_total_detections CHECK (total_detections >= 0)
);

CREATE INDEX IF NOT EXISTS idx_vision_stats_daily_tenant_date
ON vision_statistics_daily (tenant_id, period_date DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_daily_tenant_camera_date
ON vision_statistics_daily (tenant_id, camera_id, period_date DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_daily_tenant_location_date
ON vision_statistics_daily (tenant_id, location_id, period_date DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_daily_tenant_label_date
ON vision_statistics_daily (tenant_id, label, period_date DESC);
```

---

## 18. Migracion 9: estadisticas mensuales

```sql
CREATE TABLE IF NOT EXISTS vision_statistics_monthly (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    camera_id UUID NULL,
    location_id UUID NULL,
    label VARCHAR(120) NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    total_events INTEGER NOT NULL DEFAULT 0,
    total_detections INTEGER NOT NULL DEFAULT 0,
    avg_confidence NUMERIC(8,5) NULL,
    max_confidence NUMERIC(8,5) NULL,
    min_confidence NUMERIC(8,5) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vision_stats_monthly_scope UNIQUE (tenant_id, camera_id, location_id, label, year, month),
    CONSTRAINT chk_vision_stats_monthly_month CHECK (month >= 1 AND month <= 12),
    CONSTRAINT chk_vision_stats_monthly_year CHECK (year >= 2020),
    CONSTRAINT chk_vision_stats_monthly_total_events CHECK (total_events >= 0),
    CONSTRAINT chk_vision_stats_monthly_total_detections CHECK (total_detections >= 0)
);

CREATE INDEX IF NOT EXISTS idx_vision_stats_monthly_tenant_year_month
ON vision_statistics_monthly (tenant_id, year DESC, month DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_monthly_tenant_camera_year_month
ON vision_statistics_monthly (tenant_id, camera_id, year DESC, month DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_monthly_tenant_location_year_month
ON vision_statistics_monthly (tenant_id, location_id, year DESC, month DESC);

CREATE INDEX IF NOT EXISTS idx_vision_stats_monthly_tenant_label_year_month
ON vision_statistics_monthly (tenant_id, label, year DESC, month DESC);
```

---

## 19. Migracion 10: logs de procesamiento

```sql
CREATE TABLE IF NOT EXISTS vision_processing_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL,
    camera_id UUID NULL,
    event_id UUID NULL,
    request_id VARCHAR(180) NULL,
    level VARCHAR(30) NOT NULL DEFAULT 'INFO',
    code VARCHAR(120) NULL,
    message TEXT NOT NULL,
    payload JSONB NULL,
    error_detail TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vision_processing_logs_tenant_created
ON vision_processing_logs (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vision_processing_logs_event
ON vision_processing_logs (event_id);

CREATE INDEX IF NOT EXISTS idx_vision_processing_logs_level_created
ON vision_processing_logs (level, created_at DESC);
```

---

## 20. Migracion 11: auditoria Vision

```sql
CREATE TABLE IF NOT EXISTS vision_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL,
    actor_user_id UUID NULL,
    actor_type VARCHAR(60) NOT NULL DEFAULT 'SYSTEM',
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(120) NOT NULL,
    resource_id UUID NULL,
    ip_address VARCHAR(80) NULL,
    user_agent TEXT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vision_audit_logs_tenant_created
ON vision_audit_logs (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vision_audit_logs_actor_created
ON vision_audit_logs (actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_vision_audit_logs_action_created
ON vision_audit_logs (action, created_at DESC);
```

---

## 21. Migracion 12: trigger updated_at

```sql
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vision_api_keys_updated_at ON vision_api_keys;
CREATE TRIGGER trg_vision_api_keys_updated_at
BEFORE UPDATE ON vision_api_keys
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_locations_updated_at ON vision_locations;
CREATE TRIGGER trg_vision_locations_updated_at
BEFORE UPDATE ON vision_locations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_cameras_updated_at ON vision_cameras;
CREATE TRIGGER trg_vision_cameras_updated_at
BEFORE UPDATE ON vision_cameras
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_events_updated_at ON vision_events;
CREATE TRIGGER trg_vision_events_updated_at
BEFORE UPDATE ON vision_events
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_stats_hourly_updated_at ON vision_statistics_hourly;
CREATE TRIGGER trg_vision_stats_hourly_updated_at
BEFORE UPDATE ON vision_statistics_hourly
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_stats_daily_updated_at ON vision_statistics_daily;
CREATE TRIGGER trg_vision_stats_daily_updated_at
BEFORE UPDATE ON vision_statistics_daily
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vision_stats_monthly_updated_at ON vision_statistics_monthly;
CREATE TRIGGER trg_vision_stats_monthly_updated_at
BEFORE UPDATE ON vision_statistics_monthly
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

---

## 22. Migracion 13: funciones de upsert para estadisticas

### 22.1 Upsert estadistica diaria

```sql
CREATE OR REPLACE FUNCTION upsert_vision_statistics_daily(
    p_tenant_id UUID,
    p_camera_id UUID,
    p_location_id UUID,
    p_label VARCHAR,
    p_period_date DATE,
    p_total_events INTEGER,
    p_total_detections INTEGER,
    p_avg_confidence NUMERIC,
    p_max_confidence NUMERIC,
    p_min_confidence NUMERIC
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO vision_statistics_daily (
        tenant_id,
        camera_id,
        location_id,
        label,
        period_date,
        total_events,
        total_detections,
        avg_confidence,
        max_confidence,
        min_confidence
    ) VALUES (
        p_tenant_id,
        p_camera_id,
        p_location_id,
        p_label,
        p_period_date,
        p_total_events,
        p_total_detections,
        p_avg_confidence,
        p_max_confidence,
        p_min_confidence
    )
    ON CONFLICT (tenant_id, camera_id, location_id, label, period_date)
    DO UPDATE SET
        total_events = vision_statistics_daily.total_events + EXCLUDED.total_events,
        total_detections = vision_statistics_daily.total_detections + EXCLUDED.total_detections,
        avg_confidence = CASE
            WHEN vision_statistics_daily.avg_confidence IS NULL THEN EXCLUDED.avg_confidence
            WHEN EXCLUDED.avg_confidence IS NULL THEN vision_statistics_daily.avg_confidence
            ELSE (vision_statistics_daily.avg_confidence + EXCLUDED.avg_confidence) / 2
        END,
        max_confidence = GREATEST(
            COALESCE(vision_statistics_daily.max_confidence, 0),
            COALESCE(EXCLUDED.max_confidence, 0)
        ),
        min_confidence = CASE
            WHEN vision_statistics_daily.min_confidence IS NULL THEN EXCLUDED.min_confidence
            WHEN EXCLUDED.min_confidence IS NULL THEN vision_statistics_daily.min_confidence
            ELSE LEAST(vision_statistics_daily.min_confidence, EXCLUDED.min_confidence)
        END,
        updated_at = NOW();
END;
$$ LANGUAGE plpgsql;
```

### 22.2 Nota sobre promedio

El promedio anterior es aproximado si se actualiza incrementalmente. Para precision estadistica completa, usar suma acumulada y conteo.

Opcion mas precisa para produccion:

Agregar columnas:

```sql
ALTER TABLE vision_statistics_daily
ADD COLUMN IF NOT EXISTS confidence_sum NUMERIC(18,6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS confidence_count INTEGER NOT NULL DEFAULT 0;
```

Luego calcular:

```sql
avg_confidence = confidence_sum / NULLIF(confidence_count, 0)
```

Codex debe preferir esta opcion si el dashboard necesita precision real.

---

## 23. Migracion 14: columnas para promedio preciso

```sql
ALTER TABLE vision_statistics_hourly
ADD COLUMN IF NOT EXISTS confidence_sum NUMERIC(18,6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS confidence_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE vision_statistics_daily
ADD COLUMN IF NOT EXISTS confidence_sum NUMERIC(18,6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS confidence_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE vision_statistics_monthly
ADD COLUMN IF NOT EXISTS confidence_sum NUMERIC(18,6) NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS confidence_count INTEGER NOT NULL DEFAULT 0;
```

---

## 24. Migracion 15: Row Level Security opcional en Supabase/PostgreSQL

Si el proyecto usa Supabase y autenticacion con JWT, se puede activar RLS. Si el backend central ya filtra todo por tenant, RLS puede quedar como capa adicional.

No activar RLS sin probar el flujo actual, porque puede romper consultas existentes.

```sql
ALTER TABLE vision_cameras ENABLE ROW LEVEL SECURITY;
ALTER TABLE vision_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE vision_detections ENABLE ROW LEVEL SECURITY;
ALTER TABLE vision_statistics_hourly ENABLE ROW LEVEL SECURITY;
ALTER TABLE vision_statistics_daily ENABLE ROW LEVEL SECURITY;
ALTER TABLE vision_statistics_monthly ENABLE ROW LEVEL SECURITY;
```

Politica ejemplo si el JWT contiene tenant_id:

```sql
CREATE POLICY vision_cameras_tenant_isolation
ON vision_cameras
USING (tenant_id::text = current_setting('request.jwt.claims', true)::jsonb ->> 'tenant_id');
```

Codex debe verificar si el proyecto realmente usa Supabase Auth antes de aplicar esto.

---

## 25. Backend: estructura recomendada de carpetas

Si el backend es Spring Boot:

```text
src/main/java/.../vision/
  controller/
    VisionEventController.java
    VisionStatisticsController.java
    VisionCameraController.java
  dto/
    VisionEventRequest.java
    VisionEventResponse.java
    VisionDetectionRequest.java
    VisionStatisticsResponse.java
    VisionCameraResponse.java
  entity/
    VisionApiKey.java
    VisionCamera.java
    VisionEvent.java
    VisionDetection.java
    VisionStatisticsDaily.java
    VisionStatisticsHourly.java
    VisionStatisticsMonthly.java
  repository/
    VisionApiKeyRepository.java
    VisionCameraRepository.java
    VisionEventRepository.java
    VisionDetectionRepository.java
    VisionStatisticsDailyRepository.java
  service/
    VisionApiKeyService.java
    VisionTenantResolver.java
    VisionEventService.java
    VisionStatisticsService.java
    VisionValidationService.java
    VisionAuditService.java
  security/
    VisionApiKeyFilter.java
    VisionTenantContext.java
  exception/
    VisionException.java
    VisionErrorCode.java
```

Si el backend es Node/Express/NestJS:

```text
src/vision/
  controllers/
  dto/
  entities/
  repositories/
  services/
  guards/
  middleware/
  exceptions/
```

Codex debe adaptarse al framework real.

---

## 26. Logica obligatoria de resolucion de tenant

### 26.1 Pseudocodigo

```text
function resolveTenant(request):
    apiKey = request.header['X-Vision-Api-Key']

    if apiKey is empty:
        throw VISION_API_KEY_REQUIRED

    keyHash = hash(apiKey)
    record = find vision_api_keys by keyHash and status ACTIVE

    if record not found:
        throw VISION_API_KEY_INVALID

    if record.expires_at is not null and expires_at < now:
        throw VISION_API_KEY_EXPIRED

    update last_used_at

    return record.tenant_id
```

### 26.2 Regla

No usar `tenantId` del body para resolver el negocio.

---

## 27. Logica obligatoria para guardar evento

```text
1. Resolver tenant.
2. Validar request.
3. Buscar location por tenant_id + locationCode si viene locationCode.
4. Buscar camera por tenant_id + cameraCode.
5. Validar camera.status = ACTIVE.
6. Validar que camera.location_id coincida con location si aplica.
7. Revisar idempotency_key o frame_id.
8. Si existe, retornar evento ya procesado.
9. Insertar vision_events.
10. Insertar vision_detections en lote.
11. Actualizar total_detections.
12. Actualizar estadisticas hourly/daily/monthly.
13. Registrar audit log.
14. Retornar respuesta.
```

---

## 28. Reglas de validacion del payload

```text
cameraCode: requerido, texto, max 80.
locationCode: opcional, texto, max 80.
timestamp: requerido, fecha valida ISO 8601.
frameId: recomendado, max 180.
detections: requerido, arreglo, maximo configurable.
detections.label: requerido, max 120.
detections.confidence: requerido, numero entre 0 y 1.
bbox.x: requerido, numero >= 0.
bbox.y: requerido, numero >= 0.
bbox.width: requerido, numero > 0.
bbox.height: requerido, numero > 0.
image.width: opcional, entero > 0.
image.height: opcional, entero > 0.
metadata: opcional, JSON.
```

Limites recomendados:

```text
Max detections por evento: 500.
Max payload: 2 MB si solo manda JSON.
No enviar imagen base64 en este endpoint salvo que sea estrictamente necesario.
Si se necesita imagen, mandar image_uri o storage path.
```

---

## 29. DTO recomendado para request

```json
{
  "cameraCode": "string",
  "locationCode": "string|null",
  "timestamp": "ISO-8601 datetime",
  "frameId": "string|null",
  "source": "string|null",
  "model": {
    "name": "string|null",
    "version": "string|null",
    "confidenceThreshold": 0.65
  },
  "image": {
    "width": 1920,
    "height": 1080,
    "uri": "string|null"
  },
  "detections": [
    {
      "label": "string",
      "confidence": 0.94,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 220,
        "height": 410
      },
      "trackingId": "string|null",
      "metadata": {}
    }
  ],
  "metadata": {}
}
```

---

## 30. DTO recomendado para respuesta

```json
{
  "success": true,
  "eventId": "uuid",
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "receivedDetections": 10,
  "storedDetections": 10,
  "duplicate": false,
  "statisticsUpdated": true,
  "processedAt": "2026-05-02T18:00:01-05:00"
}
```

---

## 31. Actualizacion de estadisticas

### 31.1 Estrategia recomendada para produccion inicial

Para evitar complejidad excesiva al inicio:

```text
Actualizar estadisticas sincronamente despues de guardar evento y detecciones.
```

Ventaja:

```text
El dashboard refleja los datos de inmediato.
```

Desventaja:

```text
Si hay demasiados eventos por segundo, puede aumentar el tiempo de respuesta.
```

### 31.2 Estrategia escalable futura

```text
Guardar evento rapido -> publicar job/queue -> procesar estadisticas asincronamente.
```

Opciones:

```text
- Cola interna.
- Redis queue.
- RabbitMQ.
- Kafka.
- Scheduler batch.
```

Para la primera version, usar sincronico con transaccion optimizada.

---

## 32. Algoritmo de agregacion estadistica

Por cada evento:

```text
1. Agrupar detecciones por label.
2. Por cada label calcular:
   - total_detections
   - confidence_sum
   - confidence_count
   - min_confidence
   - max_confidence
3. Actualizar tabla hourly.
4. Actualizar tabla daily.
5. Actualizar tabla monthly.
```

Scope de agregacion:

```text
tenant_id + camera_id + location_id + label + periodo
```

Tambien se puede insertar agregados globales del tenant con camera_id NULL y location_id NULL.

Recomendacion:

```text
Guardar ambos:
- Estadistica por camara.
- Estadistica global del tenant.
```

---

## 33. Query ejemplo para dashboard

### 33.1 Total por label en rango

```sql
SELECT
    label,
    SUM(total_detections) AS total_detections
FROM vision_statistics_daily
WHERE tenant_id = :tenantId
AND period_date BETWEEN :fromDate AND :toDate
GROUP BY label
ORDER BY total_detections DESC;
```

### 33.2 Total por camara

```sql
SELECT
    c.code AS camera_code,
    c.name AS camera_name,
    SUM(s.total_detections) AS total_detections
FROM vision_statistics_daily s
JOIN vision_cameras c ON c.id = s.camera_id
WHERE s.tenant_id = :tenantId
AND c.tenant_id = :tenantId
AND s.period_date BETWEEN :fromDate AND :toDate
GROUP BY c.code, c.name
ORDER BY total_detections DESC;
```

### 33.3 Timeline diaria

```sql
SELECT
    period_date,
    SUM(total_detections) AS total_detections
FROM vision_statistics_daily
WHERE tenant_id = :tenantId
AND period_date BETWEEN :fromDate AND :toDate
GROUP BY period_date
ORDER BY period_date ASC;
```

---

## 34. Seguridad de endpoints

### 34.1 Endpoint de ingestion YOLO

```text
POST /api/vision/events
```

Autenticacion:

```text
X-Vision-Api-Key
```

No requiere sesion de usuario normal.

### 34.2 Endpoint dashboard/reportes

```text
GET /api/vision/statistics
GET /api/vision/events/recent
GET /api/vision/reports/detections
```

Autenticacion:

```text
JWT o auth normal del sistema.
```

Regla:

```text
tenant_id sale del usuario autenticado, no de query params.
```

### 34.3 Roles sugeridos

```text
VISION_VIEWER: puede ver estadisticas.
VISION_ADMIN: puede administrar camaras y API keys.
VISION_SYSTEM: puede insertar eventos desde servicio YOLO.
SUPER_ADMIN: puede ver varios tenants, solo si el sistema lo permite.
```

---

## 35. Control de errores

Crear codigos internos:

```text
VISION_API_KEY_REQUIRED
VISION_API_KEY_INVALID
VISION_API_KEY_EXPIRED
VISION_CAMERA_REQUIRED
VISION_CAMERA_NOT_FOUND
VISION_CAMERA_INACTIVE
VISION_CAMERA_TENANT_MISMATCH
VISION_LOCATION_NOT_FOUND
VISION_PAYLOAD_INVALID
VISION_EVENT_DUPLICATED
VISION_DETECTION_LIMIT_EXCEEDED
VISION_INTERNAL_ERROR
```

Cada error debe responder JSON estable para que el companero pueda depurar facil.

---

## 36. Idempotencia

### 36.1 Problema

El sistema YOLO puede reenviar el mismo frame/evento por error de red.

### 36.2 Solucion

Usar uno de estos:

```text
- Header Idempotency-Key.
- frameId unico por camara.
```

Restricciones ya definidas:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_events_tenant_idempotency
ON vision_events (tenant_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_events_tenant_camera_frame
ON vision_events (tenant_id, camera_id, frame_id)
WHERE frame_id IS NOT NULL;
```

### 36.3 Regla

Si llega duplicado:

```text
No insertar detecciones otra vez.
No sumar estadisticas otra vez.
Responder success true con duplicate true.
```

---

## 37. Transacciones

La operacion de guardar evento debe ser transaccional.

```text
BEGIN
  validar tenant
  validar camara
  insertar evento
  insertar detecciones
  actualizar estadisticas
COMMIT
```

Si falla algo:

```text
ROLLBACK
registrar error controlado
```

---

## 38. Rendimiento

### 38.1 Indices criticos

```text
vision_events: tenant_id + camera_id + event_timestamp
vision_events: tenant_id + event_timestamp
vision_detections: tenant_id + event_id
vision_detections: tenant_id + camera_id + label
vision_statistics_daily: tenant_id + period_date
vision_statistics_daily: tenant_id + camera_id + period_date
vision_statistics_daily: tenant_id + label + period_date
```

### 38.2 Paginacion obligatoria

Endpoints de eventos recientes y reportes deben usar paginacion.

```text
limit maximo recomendado: 100
```

### 38.3 Evitar N+1

Para eventos recientes, traer camara y location con join controlado o DTO projection.

No cargar entidades completas si solo se necesitan nombres.

### 38.4 Cache

Cachear:

```text
- Resolucion API Key -> tenant_id.
- Camaras activas por tenant + code.
- Estadisticas de dashboard por rangos cortos.
```

TTL recomendado:

```text
API Key: 1 a 5 minutos.
Camaras: 1 a 5 minutos.
Dashboard: 15 a 60 segundos.
```

Al revocar API Key, invalidar cache.

---

## 39. Dashboard

### 39.1 Tarjetas principales

```text
- Total de eventos recibidos.
- Total de detecciones.
- Camaras activas.
- Camara con mas detecciones.
- Label mas detectado.
- Promedio de confianza.
```

### 39.2 Graficos

```text
- Detecciones por dia.
- Detecciones por hora.
- Detecciones por camara.
- Detecciones por label.
- Detecciones por sucursal.
```

### 39.3 Filtros

```text
- Fecha desde.
- Fecha hasta.
- Camara.
- Sucursal.
- Label.
- Periodo: hourly/daily/monthly.
```

### 39.4 Regla multi-tenant del dashboard

El frontend no debe mandar tenantId manualmente.

El backend debe responder segun el tenant del usuario autenticado.

---

## 40. Reportes

### 40.1 Reporte de detecciones

Campos:

```text
Fecha/hora
Sucursal
Camara
Label
Total detecciones
Confianza promedio
Confianza minima
Confianza maxima
```

### 40.2 Reporte de eventos

Campos:

```text
Fecha/hora evento
Fecha/hora recibido
Camara
Sucursal
Modelo YOLO
Version modelo
Total detecciones
Tiempo de procesamiento
Estado
```

### 40.3 Exportacion

Formatos:

```text
JSON inicialmente.
CSV opcional.
Excel opcional si ya existe modulo de exportacion.
PDF opcional para despues.
```

---

## 41. Pruebas obligatorias

### 41.1 Pruebas unitarias

```text
- Resolver tenant por API Key valida.
- Rechazar API Key invalida.
- Rechazar API Key revocada.
- Rechazar API Key expirada.
- Validar camara activa.
- Rechazar camara de otro tenant.
- Validar payload con detecciones correctas.
- Rechazar confidence fuera de 0..1.
- Rechazar bbox con width/height <= 0.
- Detectar evento duplicado por Idempotency-Key.
- Detectar evento duplicado por frameId.
```

### 41.2 Pruebas de integracion

```text
- Tenant A envia evento CAM-001.
- Tenant B envia evento CAM-001.
- Verificar que ambos se guardan separados.
- Verificar dashboard de Tenant A no muestra datos de Tenant B.
- Verificar dashboard de Tenant B no muestra datos de Tenant A.
- Verificar reportes respetan tenant.
- Verificar estadisticas por camara.
- Verificar estadisticas globales del tenant.
```

### 41.3 Prueba de concurrencia

```text
- Enviar 100 eventos simultaneos para un tenant.
- Enviar 100 eventos simultaneos para otro tenant.
- Confirmar que no hay mezcla.
- Confirmar que estadisticas cuadran.
```

### 41.4 Prueba de rendimiento basica

```text
- Insertar 10.000 eventos de prueba.
- Consultar dashboard diario.
- Confirmar respuesta aceptable.
- Revisar query plan si hay lentitud.
```

---

## 42. Checklist de produccion

```text
[ ] Todas las tablas Vision tienen tenant_id.
[ ] Todas las consultas filtran por tenant_id.
[ ] El endpoint YOLO resuelve tenant desde API Key.
[ ] El endpoint YOLO no confia en tenantId del body.
[ ] La camara se valida con tenant_id + cameraCode.
[ ] Hay indices compuestos.
[ ] Hay idempotencia.
[ ] Hay logs controlados.
[ ] Hay errores JSON claros.
[ ] Hay pruebas de aislamiento multi-tenant.
[ ] El dashboard consume estadisticas filtradas por tenant.
[ ] Los reportes consumen datos filtrados por tenant.
[ ] No existe endpoint publico que permita pasar tenantId libremente.
[ ] API keys se guardan con hash, no en texto plano.
[ ] Hay forma de revocar API keys.
[ ] Hay paginacion.
[ ] Hay limites de payload.
[ ] No se envia imagen base64 pesada salvo necesidad real.
```

---

## 43. Prompt maestro para Codex con GPT-5.5

Copiar y pegar este bloque completo en Codex dentro del proyecto abierto en WebStorm / IntelliJ IDEA.

```text
Necesito que implementes el modulo Vision AI multi-tenant en este proyecto.

Contexto:
El sistema debe recibir eventos de deteccion desde un servicio YOLO externo desarrollado por un companero. El endpoint debe recibir detecciones de camaras, guardar eventos, guardar detecciones, actualizar estadisticas y exponer datos para dashboards y reportes. El sistema debe estar preparado para multiples negocios/tenants funcionando en simultaneo sin mezclar informacion.

Objetivo principal:
Crear un modulo Vision AI seguro, escalable, ordenado y conectado con los modulos actuales del sistema: tenants, usuarios, dashboard, reportes, camaras/sucursales si ya existen.

Primero inspecciona el proyecto y confirma internamente:
- Framework backend real.
- Motor de base de datos.
- Sistema de migraciones.
- Tablas existentes relacionadas con tenants, usuarios, sucursales, camaras, dashboards y reportes.
- Estructura de carpetas y convenciones del codigo.

Reglas obligatorias:
1. No dupliques tablas si ya existen equivalentes.
2. Toda tabla nueva relacionada con Vision AI debe tener tenant_id.
3. Toda consulta del modulo Vision AI debe filtrar por tenant_id.
4. El endpoint de ingreso YOLO no debe confiar en tenantId enviado en el body.
5. El tenant debe resolverse desde X-Vision-Api-Key o desde el mecanismo de autenticacion existente si aplica.
6. La API Key debe guardarse como hash, nunca en texto plano.
7. Cada camara debe validarse usando tenant_id + cameraCode.
8. Si una camara no pertenece al tenant resuelto, rechazar la solicitud.
9. Debe existir idempotencia por Idempotency-Key o por tenant_id + camera_id + frame_id.
10. No sumar estadisticas dos veces si el evento esta duplicado.
11. Las estadisticas deben estar separadas por tenant, camara, sucursal/location, label y periodo.
12. Crear indices compuestos para tenant_id, camera_id, location_id, label y fechas.
13. Crear errores JSON estables para que el companero pueda depurar.
14. Crear endpoints para dashboard y reportes.
15. Crear pruebas de aislamiento multi-tenant.
16. No romper modulos existentes.
17. Mantener el estilo del codigo actual del proyecto.
18. Evitar consultas N+1.
19. Usar paginacion en listados.
20. Preparar el codigo para produccion.

Endpoint externo para YOLO:
POST /api/vision/events
Headers:
Content-Type: application/json
X-Vision-Api-Key: vk_live_xxxxxxxxxxxxxxxxxxxxx
Idempotency-Key: opcional

Body esperado:
{
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "timestamp": "2026-05-02T18:00:00-05:00",
  "frameId": "frame-000123",
  "source": "yolo-service",
  "model": {
    "name": "YOLO",
    "version": "v8-or-custom",
    "confidenceThreshold": 0.65
  },
  "image": {
    "width": 1920,
    "height": 1080,
    "uri": null
  },
  "detections": [
    {
      "label": "persona",
      "confidence": 0.94,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 220,
        "height": 410
      },
      "trackingId": "track-001",
      "metadata": {}
    }
  ],
  "metadata": {
    "device": "vision-ai-camera",
    "fps": 15,
    "processingMs": 48
  }
}

Respuesta esperada:
{
  "success": true,
  "eventId": "uuid-event-id",
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "receivedDetections": 1,
  "storedDetections": 1,
  "duplicate": false,
  "statisticsUpdated": true,
  "processedAt": "2026-05-02T18:00:01-05:00"
}

Endpoints internos:
GET /api/vision/statistics?from=YYYY-MM-DD&to=YYYY-MM-DD&period=daily
GET /api/vision/cameras/{cameraCode}/statistics?from=YYYY-MM-DD&to=YYYY-MM-DD&period=hourly
GET /api/vision/events/recent?limit=50
GET /api/vision/reports/detections?from=YYYY-MM-DD&to=YYYY-MM-DD&format=json

Regla para endpoints internos:
El tenant debe salir del usuario autenticado. No permitir que un usuario comun consulte tenantId de otro negocio por query param.

Migraciones:
Implementa migraciones equivalentes a estas tablas, adaptando nombres si el proyecto ya tiene tablas existentes:
- vision_api_keys
- vision_locations si no existe tabla equivalente
- vision_cameras si no existe tabla equivalente
- vision_events
- vision_detections
- vision_statistics_hourly
- vision_statistics_daily
- vision_statistics_monthly
- vision_processing_logs
- vision_audit_logs

Incluye indices:
- vision_events (tenant_id, camera_id, event_timestamp DESC)
- vision_events (tenant_id, event_timestamp DESC)
- vision_detections (tenant_id, event_id)
- vision_detections (tenant_id, camera_id, label)
- vision_statistics_daily (tenant_id, period_date DESC)
- vision_statistics_daily (tenant_id, camera_id, period_date DESC)
- vision_statistics_daily (tenant_id, label, period_date DESC)

Servicios requeridos:
- VisionTenantResolver
- VisionApiKeyService
- VisionValidationService
- VisionEventService
- VisionStatisticsService
- VisionAuditService

Logica de guardado:
1. Resolver tenant desde API key.
2. Validar payload.
3. Buscar camara por tenant_id + cameraCode.
4. Validar camara activa.
5. Buscar location si se envia locationCode.
6. Verificar duplicado por idempotency key o frameId.
7. Guardar evento.
8. Guardar detecciones en lote.
9. Actualizar estadisticas hourly/daily/monthly.
10. Retornar respuesta.

Pruebas minimas:
- Tenant A y Tenant B pueden tener CAM-001 sin mezclarse.
- Tenant A no puede insertar evento para camara de Tenant B.
- Dashboard de Tenant A no muestra datos de Tenant B.
- Evento duplicado no duplica estadisticas.
- API key invalida rechaza request.
- Payload invalido devuelve error JSON claro.

Criterio de aceptacion:
El modulo debe compilar, migrar base de datos, pasar pruebas y dejar endpoints funcionales para que el companero consuma POST /api/vision/events y el dashboard consuma GET /api/vision/statistics sin mezclar datos entre negocios.
```

---

## 44. Prompt para GPT-5.5 antes de implementar

Usar en ChatGPT para revisar arquitectura antes de ejecutar en Codex:

```text
Revisa este plan de modulo Vision AI multi-tenant. Necesito confirmar que el flujo impide mezcla de datos entre tenants, que el endpoint para YOLO esta bien definido, que las estadisticas sirven para dashboard/reportes y que la base de datos esta preparada para produccion. Senala solo riesgos tecnicos reales y correcciones necesarias.
```

---

## 45. Prompt para Opus 4.7 como auditor

Usar despues de que Codex implemente:

```text
Actua como auditor tecnico de arquitectura multi-tenant. Revisa este modulo Vision AI ya implementado. Busca especificamente:

1. Consultas sin tenant_id.
2. Endpoints que permitan consultar tenantId por query param sin control.
3. Posibles fugas de datos entre negocios.
4. Estadisticas que puedan mezclarse entre tenants.
5. Problemas de idempotencia.
6. Problemas de concurrencia.
7. Indices faltantes.
8. Payloads que puedan tumbar produccion.
9. API keys mal almacenadas.
10. Falta de logs o auditoria.

Devuelve hallazgos concretos con archivo, linea aproximada, riesgo, impacto y correccion sugerida.
```

---

## 46. Prompt final para Codex despues de auditoria

```text
Aplica las correcciones de auditoria del modulo Vision AI. No cambies comportamiento que ya funciona. Corrige solamente riesgos reales: aislamiento tenant, seguridad, idempotencia, rendimiento, errores JSON, pruebas y endpoints. Despues ejecuta pruebas y deja un resumen de archivos modificados.
```

---

## 47. Contrato corto para enviar al companero de YOLO

```markdown
# Contrato de integracion YOLO -> Vision AI

## Endpoint

POST /api/vision/events

## Headers

Content-Type: application/json
X-Vision-Api-Key: API_KEY_ENTREGADA_POR_EL_ADMIN
Idempotency-Key: opcional-pero-recomendado

## Body

```json
{
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "timestamp": "2026-05-02T18:00:00-05:00",
  "frameId": "frame-000123",
  "source": "yolo-service",
  "model": {
    "name": "YOLO",
    "version": "v8",
    "confidenceThreshold": 0.65
  },
  "image": {
    "width": 1920,
    "height": 1080,
    "uri": null
  },
  "detections": [
    {
      "label": "persona",
      "confidence": 0.94,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 220,
        "height": 410
      },
      "trackingId": "track-001"
    }
  ],
  "metadata": {
    "processingMs": 48
  }
}
```

## Respuesta success

```json
{
  "success": true,
  "eventId": "uuid-event-id",
  "cameraCode": "CAM-001",
  "locationCode": "SUC-001",
  "receivedDetections": 1,
  "storedDetections": 1,
  "duplicate": false,
  "statisticsUpdated": true,
  "processedAt": "2026-05-02T18:00:01-05:00"
}
```

## Reglas

- No enviar tenantId.
- El tenant se resuelve con la API Key.
- cameraCode debe existir para ese negocio.
- frameId debe ser unico por camara.
- confidence debe ir entre 0 y 1.
- bbox.width y bbox.height deben ser mayores a 0.
```

---

## 48. Verificacion final

Antes de cerrar la tarea, Codex debe entregar:

```text
- Lista de migraciones creadas.
- Lista de endpoints creados.
- Lista de servicios creados.
- Como generar API Key para un tenant.
- Ejemplo curl para YOLO.
- Ejemplo de respuesta.
- Pruebas ejecutadas.
- Riesgos pendientes si existen.
```

---

## 49. Ejemplo curl para probar

```bash
curl -X POST "https://tu-dominio.com/api/vision/events" \
  -H "Content-Type: application/json" \
  -H "X-Vision-Api-Key: vk_live_xxxxxxxxxxxxxxxxxxxxx" \
  -H "Idempotency-Key: tenant-a-cam-001-frame-000123" \
  -d '{
    "cameraCode": "CAM-001",
    "locationCode": "SUC-001",
    "timestamp": "2026-05-02T18:00:00-05:00",
    "frameId": "frame-000123",
    "source": "yolo-service",
    "model": {
      "name": "YOLO",
      "version": "v8",
      "confidenceThreshold": 0.65
    },
    "image": {
      "width": 1920,
      "height": 1080,
      "uri": null
    },
    "detections": [
      {
        "label": "persona",
        "confidence": 0.94,
        "bbox": {
          "x": 120,
          "y": 80,
          "width": 220,
          "height": 410
        },
        "trackingId": "track-001"
      }
    ],
    "metadata": {
      "processingMs": 48
    }
  }'
```

---

## 50. Modulo de correos de facturacion con Resend

### 50.1 Objetivo

Dejar completamente terminado el flujo de correos usados para facturacion electronica, RIDE, XML, reenvios, trazabilidad, dashboard y reportes.

Correo oficial obligatorio:

```text
soporte@insightvisionia.cloud
```

Proveedor principal:

```text
Resend
```

Proveedor secundario:

```text
SMTP fallback existente por tenant/default, solo si ya esta implementado y no rompe el flujo actual.
```

---

## 51. Contexto tecnico detectado del proyecto

Datos detectados del diagnostico entregado:

```text
Backend: Spring Boot 3.5.7
Lenguaje: Java 17
Gestor backend: Maven
Monorepo/frontend: npm
Variables: application.yml + perfiles + optional:file:.env[.properties]
Migraciones: Flyway
Base principal: PostgreSQL
Autenticacion: JWT stateless + Firebase token
Multi-tenant: si, con tenant_id, TenantContextResolver y header X-Tenant-Code
Facturacion: si, modulo SRI/RIDE existente
Clientes: si, PartyService/PartyController y CustomerDocumentsController
Reportes: si, ErpReportsController/ErpReportsService
Correos: si, Resend + SMTP fallback, pero incompleto para produccion
```

Archivos importantes existentes:

```text
apps/api/src/main/java/com/ucacue/bar/service/EmailService.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/email/ResendEmailClient.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/application/SriAutomationService.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/application/SriDocumentService.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/application/SriRideService.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/application/RideStorageService.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/config/SriProperties.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/TaxDocumentEntity.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/TenantEmailSettingsEntity.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/RideTemplateEntity.java
apps/api/src/main/java/com/ucacue/bar/service/NotificationService.java
apps/api/src/main/resources/application.yml
apps/api/src/main/resources/application-example.yml
apps/api/.env.example
apps/api/src/main/resources/db/migration/postgresql/V20__ride_automation.sql
apps/api/src/main/resources/db/migration/postgresql/V21__ride_email_retry_state.sql
apps/api/src/main/resources/db/migration/postgresql/V22__ride_xml_storage.sql
apps/api/src/main/resources/db/migration/postgresql/V16__ride_templates.sql
```

Regla:

```text
Codex no debe duplicar funcionalidad existente. Debe extender y cerrar el flujo actual.
```

---

## 52. Estado actual detectado de facturacion y correos

### 52.1 Ya existe

```text
- Facturas SRI en tax_documents.
- Emision, validacion y autorizacion SRI.
- XML firmado.
- RIDE PDF.
- Estados SRI.
- Trazabilidad de transmisiones.
- Reintentos SRI.
- ResendEmailClient con HTTP propio hacia Resend.
- EmailService que orquesta Resend o SMTP fallback.
- SriAutomationService para flujo post-autorizacion.
- Adjuntos PDF/XML cuando estan disponibles.
- Campos resumen en tax_documents:
  - ride_email_status
  - ride_email_error
  - ride_email_attempts
  - ride_email_next_retry_at
  - ride_email_sent_at
- Scheduler de reintentos cada 60 segundos.
- Maximo actual de 3 intentos.
- Delay actual aproximado de 5 minutos para errores retryable.
- tenant_email_settings.
- tax_ride_templates.
- GET /erp/sri/health con informacion operativa basica.
```

### 52.2 Falta para produccion

```text
- Alinear remitente oficial a soporte@insightvisionia.cloud.
- Guardar message_id de Resend.
- Crear historial detallado por intento/envio.
- Crear tabla invoice_email_logs.
- Crear tabla resend_webhook_events.
- Agregar webhook de Resend.
- Agregar RESEND_REPLY_TO.
- Agregar RESEND_WEBHOOK_SECRET.
- Crear endpoint de test Resend.
- Crear endpoint de historial por factura.
- Crear endpoint de reenvio explicito.
- Crear endpoints de configuracion de correo por tenant.
- Crear plantillas HTML versionadas para factura emitida/reenvio/alerta.
- Sacar cuerpo hardcodeado de SriAutomationService.buildEmailBody.
- Hacer que EmailService.send general no sea solo log/debug si se usa para notificaciones reales.
- Controlar limite de tamano total de adjuntos.
- Evitar spam/reenvios abusivos.
- Mostrar metricas de correos en dashboard/reportes.
```

---

## 53. Configuracion obligatoria de Resend

### 53.1 Variables requeridas

Agregar o corregir en `.env.example`, `application-example.yml` y `application.yml` si corresponde:

```properties
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxx
RESEND_BASE_URL=https://api.resend.com
RESEND_FROM_EMAIL=soporte@insightvisionia.cloud
RESEND_FROM_NAME=Insight Vision IA
RESEND_REPLY_TO=soporte@insightvisionia.cloud
RESEND_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxx
RESEND_CONNECT_TIMEOUT_MS=5000
RESEND_READ_TIMEOUT_MS=15000
```

### 53.2 Regla del remitente

El remitente global por defecto debe ser:

```text
soporte@insightvisionia.cloud
```

No deben quedar valores por defecto antiguos como:

```text
facturas@insightvision.com
facturas@example.com
no-reply@example.com
```

Si existen en archivos de ejemplo, reemplazarlos por:

```text
soporte@insightvisionia.cloud
```

### 53.3 From, Reply-To y soporte

Configuracion recomendada:

```text
From email: soporte@insightvisionia.cloud
From name: Insight Vision IA
Reply-To: soporte@insightvisionia.cloud
Correo visible al cliente: soporte@insightvisionia.cloud
Correo de soporte global: soporte@insightvisionia.cloud
Correo por tenant: opcional, solo si tenant_email_settings lo permite y esta validado
```

Regla:

```text
Por defecto todos los tenants usan soporte@insightvisionia.cloud, salvo que exista configuracion tenant_email_settings activa y validada.
```

---

## 54. Migracion de correos 1: logs de envios de facturas

Archivo sugerido Flyway PostgreSQL:

```text
apps/api/src/main/resources/db/migration/postgresql/VXX__invoice_email_logs.sql
```

SQL:

```sql
CREATE TABLE IF NOT EXISTS invoice_email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tax_document_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    provider VARCHAR(40) NOT NULL DEFAULT 'RESEND',
    provider_message_id VARCHAR(180) NULL,
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(180) NULL,
    reply_to_email VARCHAR(255) NULL,
    to_email VARCHAR(255) NOT NULL,
    subject TEXT NOT NULL,
    template_code VARCHAR(120) NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    error_code VARCHAR(120) NULL,
    error_message TEXT NULL,
    retryable BOOLEAN NOT NULL DEFAULT false,
    attachment_count INTEGER NOT NULL DEFAULT 0,
    attachment_total_bytes BIGINT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ NULL,
    delivered_at TIMESTAMPTZ NULL,
    bounced_at TIMESTAMPTZ NULL,
    complained_at TIMESTAMPTZ NULL,
    opened_at TIMESTAMPTZ NULL,
    clicked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_invoice_email_logs_tax_document
        FOREIGN KEY (tax_document_id)
        REFERENCES tax_documents(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_invoice_email_logs_attempt
        CHECK (attempt_number > 0),
    CONSTRAINT chk_invoice_email_logs_attachment_count
        CHECK (attachment_count >= 0),
    CONSTRAINT chk_invoice_email_logs_attachment_total_bytes
        CHECK (attachment_total_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_invoice_email_logs_tenant_document_created
ON invoice_email_logs (tenant_id, tax_document_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_invoice_email_logs_tenant_status_created
ON invoice_email_logs (tenant_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_invoice_email_logs_provider_message
ON invoice_email_logs (provider_message_id)
WHERE provider_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_email_logs_tenant_to_email
ON invoice_email_logs (tenant_id, to_email);

DROP TRIGGER IF EXISTS trg_invoice_email_logs_updated_at ON invoice_email_logs;
CREATE TRIGGER trg_invoice_email_logs_updated_at
BEFORE UPDATE ON invoice_email_logs
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

Estados recomendados:

```text
PENDING
SENT
DELIVERED
BOUNCED
COMPLAINED
OPENED
CLICKED
FAILED
RETRY_PENDING
```

---

## 55. Migracion de correos 2: eventos webhook de Resend

Archivo sugerido:

```text
apps/api/src/main/resources/db/migration/postgresql/VXX__resend_webhook_events.sql
```

SQL:

```sql
CREATE TABLE IF NOT EXISTS resend_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL,
    invoice_email_log_id UUID NULL,
    tax_document_id UUID NULL,
    provider_message_id VARCHAR(180) NULL,
    event_type VARCHAR(80) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    signature_valid BOOLEAN NOT NULL DEFAULT false,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ NULL,
    processing_error TEXT NULL,
    CONSTRAINT fk_resend_webhook_invoice_email_log
        FOREIGN KEY (invoice_email_log_id)
        REFERENCES invoice_email_logs(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_resend_webhook_tax_document
        FOREIGN KEY (tax_document_id)
        REFERENCES tax_documents(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_resend_webhook_provider_message
ON resend_webhook_events (provider_message_id)
WHERE provider_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_resend_webhook_tenant_received
ON resend_webhook_events (tenant_id, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_resend_webhook_event_type_received
ON resend_webhook_events (event_type, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_resend_webhook_payload_gin
ON resend_webhook_events USING GIN (payload_json);
```

---

## 56. Migracion de correos 3: columnas faltantes en tax_documents

Archivo sugerido:

```text
apps/api/src/main/resources/db/migration/postgresql/VXX__tax_documents_email_tracking.sql
```

SQL:

```sql
ALTER TABLE tax_documents
ADD COLUMN IF NOT EXISTS ride_email_last_attempt_at TIMESTAMPTZ NULL,
ADD COLUMN IF NOT EXISTS ride_email_provider VARCHAR(40) NULL,
ADD COLUMN IF NOT EXISTS ride_email_message_id VARCHAR(180) NULL,
ADD COLUMN IF NOT EXISTS ride_email_template_code VARCHAR(120) NULL;

CREATE INDEX IF NOT EXISTS idx_tax_documents_tenant_ride_email_status
ON tax_documents (tenant_id, ride_email_status);

CREATE INDEX IF NOT EXISTS idx_tax_documents_tenant_ride_email_next_retry
ON tax_documents (tenant_id, ride_email_next_retry_at)
WHERE ride_email_next_retry_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tax_documents_ride_email_message
ON tax_documents (ride_email_message_id)
WHERE ride_email_message_id IS NOT NULL;
```

---

## 57. Migracion de correos 4: plantillas de email por tenant

Archivo sugerido:

```text
apps/api/src/main/resources/db/migration/postgresql/VXX__email_templates.sql
```

SQL:

```sql
CREATE TABLE IF NOT EXISTS email_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL,
    template_code VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    subject_template TEXT NOT NULL,
    html_template TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_email_templates_scope_code_version UNIQUE (tenant_id, template_code, version),
    CONSTRAINT chk_email_templates_version CHECK (version > 0)
);

CREATE INDEX IF NOT EXISTS idx_email_templates_tenant_code_active
ON email_templates (tenant_id, template_code, active);

CREATE INDEX IF NOT EXISTS idx_email_templates_global_code_active
ON email_templates (template_code, active)
WHERE tenant_id IS NULL;

DROP TRIGGER IF EXISTS trg_email_templates_updated_at ON email_templates;
CREATE TRIGGER trg_email_templates_updated_at
BEFORE UPDATE ON email_templates
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

### 57.1 Plantillas iniciales

Insert global recomendado:

```sql
INSERT INTO email_templates (
    tenant_id,
    template_code,
    name,
    subject_template,
    html_template,
    active,
    version
)
SELECT
    NULL,
    'SRI_INVOICE_ISSUED',
    'Factura electronica emitida',
    'Factura electronica {{documentNumber}} - {{issuerName}}',
    '<!doctype html><html><body style="font-family:Arial,sans-serif;background:#f6f7fb;padding:24px;color:#111827"><table width="100%" cellpadding="0" cellspacing="0" style="max-width:640px;margin:auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb"><tr><td style="padding:24px;background:#111827;color:#ffffff"><h1 style="margin:0;font-size:20px">Factura electronica</h1><p style="margin:8px 0 0;color:#d1d5db">{{issuerName}}</p></td></tr><tr><td style="padding:24px"><p>Hola {{customerName}},</p><p>Adjuntamos tu comprobante electronico correspondiente a la factura <strong>{{documentNumber}}</strong>.</p><table width="100%" cellpadding="0" cellspacing="0" style="margin:20px 0;border-collapse:collapse"><tr><td style="padding:8px;border-bottom:1px solid #e5e7eb">Fecha</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;text-align:right">{{issueDate}}</td></tr><tr><td style="padding:8px;border-bottom:1px solid #e5e7eb">Total</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;text-align:right"><strong>{{totalAmount}}</strong></td></tr></table><p>Este correo fue enviado automaticamente desde Insight Vision IA.</p><p style="font-size:12px;color:#6b7280">Si necesitas soporte, responde a este correo o escribe a soporte@insightvisionia.cloud.</p></td></tr></table></body></html>',
    true,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM email_templates
    WHERE tenant_id IS NULL
    AND template_code = 'SRI_INVOICE_ISSUED'
    AND version = 1
);
```

Plantillas requeridas:

```text
SRI_INVOICE_ISSUED
SRI_INVOICE_RESEND
SRI_INVOICE_SEND_FAILED_ADMIN
```

---

## 58. Cambios en SriProperties

Archivo:

```text
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/config/SriProperties.java
```

Agregar propiedades faltantes bajo `app.sri.ride.resend`:

```text
replyTo
webhookSecret
maxAttachmentBytes
maxTotalAttachmentBytes
```

Valores recomendados:

```yaml
app:
  sri:
    ride:
      resend:
        enabled: ${RESEND_ENABLED:true}
        api-key: ${RESEND_API_KEY:}
        base-url: ${RESEND_BASE_URL:https://api.resend.com}
        from-email: ${RESEND_FROM_EMAIL:soporte@insightvisionia.cloud}
        from-name: ${RESEND_FROM_NAME:Insight Vision IA}
        reply-to: ${RESEND_REPLY_TO:soporte@insightvisionia.cloud}
        webhook-secret: ${RESEND_WEBHOOK_SECRET:}
        connect-timeout-ms: ${RESEND_CONNECT_TIMEOUT_MS:5000}
        read-timeout-ms: ${RESEND_READ_TIMEOUT_MS:15000}
        max-attachment-bytes: ${RESEND_MAX_ATTACHMENT_BYTES:10000000}
        max-total-attachment-bytes: ${RESEND_MAX_TOTAL_ATTACHMENT_BYTES:20000000}
```

---

## 59. Cambios en ResendEmailClient

Archivo:

```text
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/email/ResendEmailClient.java
```

Debe dejar de devolver solo boolean/void si actualmente lo hace.

Debe devolver un resultado estructurado:

```text
provider: RESEND
success: true/false
messageId: id devuelto por Resend
statusCode: HTTP status
errorCode: codigo si falla
errorMessage: mensaje si falla
retryable: true/false
rawResponse: opcional controlado
```

Clase sugerida:

```text
ResendSendResult
```

Campos:

```text
boolean success
String provider
String messageId
int statusCode
String errorCode
String errorMessage
boolean retryable
Instant sentAt
```

### 59.1 Parseo de respuesta Resend

Resend normalmente devuelve un JSON con `id` cuando el envio se acepta.

Codex debe parsear el cuerpo y guardar ese valor en:

```text
provider_message_id
ride_email_message_id
```

### 59.2 Reply-To

El cliente debe enviar `reply_to` o el campo equivalente segun el contrato HTTP usado por Resend.

No exponer API Key en logs.

---

## 60. Cambios en EmailService

Archivo:

```text
apps/api/src/main/java/com/ucacue/bar/service/EmailService.java
```

Debe resolver configuracion en este orden:

```text
1. TenantEmailSettingsEntity activa, si existe y esta permitida.
2. Configuracion global Resend en SriProperties.
3. SMTP fallback existente, si Resend esta desactivado o falla de forma retryable y el fallback esta habilitado.
```

Reglas:

```text
- From global por defecto: soporte@insightvisionia.cloud.
- Reply-To global por defecto: soporte@insightvisionia.cloud.
- No loggear API keys.
- No enviar si el destinatario no es email valido.
- No enviar adjuntos que excedan limite configurado.
- Devolver resultado estructurado del proveedor.
```

Resultado sugerido:

```text
EmailSendResult
```

Campos:

```text
boolean success
String provider
String providerMessageId
String fromEmail
String fromName
String replyToEmail
String toEmail
String subject
String status
String errorCode
String errorMessage
boolean retryable
Instant sentAt
long attachmentTotalBytes
int attachmentCount
```

---

## 61. Cambios en SriAutomationService

Archivo:

```text
apps/api/src/main/java/com/ucacue/bar/erp/accounting/application/SriAutomationService.java
```

Cambios obligatorios:

```text
1. Reemplazar cuerpo hardcodeado por EmailTemplateService.
2. Usar plantilla SRI_INVOICE_ISSUED para envio automatico.
3. Usar plantilla SRI_INVOICE_RESEND para reenvio manual si aplica.
4. Adjuntar PDF/RIDE si existe.
5. Adjuntar XML si existe.
6. Guardar invoice_email_logs por cada intento.
7. Guardar provider_message_id de Resend.
8. Actualizar tax_documents:
   - ride_email_status
   - ride_email_error
   - ride_email_attempts
   - ride_email_next_retry_at
   - ride_email_sent_at
   - ride_email_last_attempt_at
   - ride_email_provider
   - ride_email_message_id
   - ride_email_template_code
9. Mantener reintentos actuales, pero registrando cada intento.
10. Evitar duplicar envios si la factura ya esta SENT/DELIVERED salvo endpoint de reenvio explicito.
```

Estados resumen recomendados para `tax_documents.ride_email_status`:

```text
PENDING
SENT
DELIVERED
FAILED
RETRY_PENDING
BOUNCED
COMPLAINED
```

---

## 62. Nuevas entidades y repositorios

Crear:

```text
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/InvoiceEmailLogEntity.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/ResendWebhookEventEntity.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/entity/EmailTemplateEntity.java
```

Crear repositorios:

```text
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/repository/InvoiceEmailLogRepository.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/repository/ResendWebhookEventRepository.java
apps/api/src/main/java/com/ucacue/bar/erp/accounting/infrastructure/persistence/repository/EmailTemplateRepository.java
```

Regla:

```text
Toda consulta debe filtrar por tenant_id cuando aplique.
```

---

## 63. Nuevos servicios

Crear:

```text
InvoiceEmailLogService
EmailTemplateService
TenantEmailSettingsService
ResendWebhookService
InvoiceEmailCommandService
```

### 63.1 InvoiceEmailLogService

Responsabilidades:

```text
- Crear log PENDING antes del intento o al iniciar envio.
- Marcar log SENT si Resend acepta.
- Marcar log FAILED si falla.
- Actualizar provider_message_id.
- Actualizar estados por webhook.
- Consultar historial por factura y tenant.
- Consultar logs por tenant con filtros.
```

### 63.2 EmailTemplateService

Responsabilidades:

```text
- Buscar plantilla activa por tenant_id + template_code.
- Si no hay plantilla tenant, usar plantilla global tenant_id NULL.
- Renderizar variables.
- Evitar expresiones peligrosas.
- Validar que subject y html resulten no vacios.
```

Variables minimas:

```text
{{issuerName}}
{{issuerRuc}}
{{customerName}}
{{customerEmail}}
{{documentNumber}}
{{issueDate}}
{{totalAmount}}
{{authorizationNumber}}
{{supportEmail}}
```

### 63.3 TenantEmailSettingsService

Responsabilidades:

```text
- Leer configuracion de correo por tenant.
- Actualizar configuracion si rol autorizado.
- Validar from_email/reply_to_email.
- Probar envio con endpoint test.
- No permitir que un tenant use dominios no verificados si no esta habilitado.
```

### 63.4 ResendWebhookService

Responsabilidades:

```text
- Validar firma usando RESEND_WEBHOOK_SECRET.
- Guardar payload en resend_webhook_events.
- Resolver provider_message_id.
- Buscar invoice_email_logs.provider_message_id.
- Actualizar invoice_email_logs.
- Actualizar resumen en tax_documents.
```

### 63.5 InvoiceEmailCommandService

Responsabilidades:

```text
- Envio manual de factura.
- Reenvio explicito de factura.
- Validacion de permisos.
- Validacion tenant.
- Rate limit basico.
- Control de estados.
```

---

## 64. Nuevos endpoints de correos de facturacion

### 64.1 Enviar factura manualmente

```http
POST /erp/sri/documents/{id}/email/send
```

Uso:

```text
Enviar correo si esta pendiente o fallo previamente.
```

Reglas:

```text
- Resolver tenant desde usuario autenticado.
- Buscar tax_document por tenant_id + id.
- Validar permiso.
- Validar que la factura exista.
- Validar destinatario.
- Enviar con plantilla SRI_INVOICE_ISSUED.
- Registrar invoice_email_logs.
```

### 64.2 Reenviar factura explicitamente

```http
POST /erp/sri/documents/{id}/email/resend
```

Uso:

```text
Reenviar aunque ya este SENT/DELIVERED, si el usuario tiene permiso.
```

Body opcional:

```json
{
  "toEmail": "cliente@correo.com",
  "reason": "Cliente solicito reenvio"
}
```

Reglas:

```text
- Registrar nuevo intento.
- Usar plantilla SRI_INVOICE_RESEND.
- Guardar auditoria.
- Aplicar limite anti-spam.
```

### 64.3 Historial por factura

```http
GET /erp/sri/documents/{id}/email/history
```

Respuesta:

```json
{
  "documentId": "uuid",
  "emailStatus": "SENT",
  "items": [
    {
      "id": "uuid",
      "attemptNumber": 1,
      "provider": "RESEND",
      "providerMessageId": "resend-message-id",
      "fromEmail": "soporte@insightvisionia.cloud",
      "toEmail": "cliente@correo.com",
      "subject": "Factura electronica 001-001-000000123",
      "templateCode": "SRI_INVOICE_ISSUED",
      "status": "SENT",
      "errorMessage": null,
      "createdAt": "2026-05-02T18:00:00-05:00",
      "sentAt": "2026-05-02T18:00:01-05:00"
    }
  ]
}
```

### 64.4 Logs por tenant

```http
GET /erp/sri/email/logs?from=2026-05-01&to=2026-05-31&status=FAILED&limit=50
```

Reglas:

```text
- Filtrar por tenant_id del usuario autenticado.
- Paginacion obligatoria.
- No devolver logs de otro tenant.
```

### 64.5 Configuracion de correo

```http
GET /erp/sri/email/settings
PUT /erp/sri/email/settings
POST /erp/sri/email/settings/test
```

Reglas:

```text
- Solo ADMIN o rol autorizado.
- No devolver secretos completos.
- Para test, enviar correo de prueba al usuario autenticado o correo indicado.
```

### 64.6 Webhook Resend

```http
POST /integrations/resend/webhook
```

Reglas:

```text
- Validar firma con RESEND_WEBHOOK_SECRET.
- No requiere JWT.
- Debe ser endpoint publico protegido por firma.
- Guardar payload.
- Correlacionar por provider_message_id.
- Actualizar estados.
```

---

## 65. Dashboard y reportes de correos

Agregar metricas a reportes/dashboard:

```text
- Correos enviados.
- Correos fallidos.
- Correos pendientes.
- Correos en reintento.
- Correos entregados si webhook esta activo.
- Correos rebotados.
- Correos con queja/spam.
- Promedio de intentos por factura.
```

Queries base:

```sql
SELECT status, COUNT(*) AS total
FROM invoice_email_logs
WHERE tenant_id = :tenantId
AND created_at BETWEEN :from AND :to
GROUP BY status
ORDER BY total DESC;
```

```sql
SELECT DATE(created_at) AS date, COUNT(*) AS total
FROM invoice_email_logs
WHERE tenant_id = :tenantId
AND created_at BETWEEN :from AND :to
GROUP BY DATE(created_at)
ORDER BY date ASC;
```

Endpoint sugerido:

```http
GET /erp/sri/email/statistics?from=YYYY-MM-DD&to=YYYY-MM-DD
```

---

## 66. Seguridad del modulo de correos

Reglas obligatorias:

```text
- RESEND_API_KEY solo en backend.
- RESEND_API_KEY nunca debe exponerse al frontend.
- No loggear API Key.
- No devolver secretos en endpoints.
- Validar tenant_id en todas las consultas.
- Validar permisos para reenviar facturas.
- Validar que la factura pertenezca al tenant autenticado.
- Validar formato de correo destinatario.
- Limitar reenvios por factura y por usuario.
- Registrar auditoria de envio manual y reenvio.
- Validar firma del webhook Resend.
- Controlar tamano total de adjuntos.
```

Limites recomendados:

```text
Max reenvios manuales por factura por hora: 3
Max adjuntos por correo: 2 inicialmente, PDF + XML
Max total adjuntos: 20 MB
Max destinatarios por envio de factura: 1 principal
```

---

## 67. Pruebas obligatorias del modulo de correos

### 67.1 Unitarias

```text
- ResendEmailClient parsea message_id correctamente.
- ResendEmailClient no loggea API Key.
- EmailService usa soporte@insightvisionia.cloud por defecto.
- EmailService usa reply-to correcto.
- EmailTemplateService usa plantilla tenant si existe.
- EmailTemplateService usa plantilla global si no existe tenant.
- SriAutomationService registra invoice_email_logs en envio exitoso.
- SriAutomationService registra invoice_email_logs en fallo.
- SriAutomationService actualiza tax_documents con message_id.
- Reenvio explicito crea nuevo intento.
- Evento duplicado/no permitido no envia correo doble.
```

### 67.2 Integracion

```text
- Tenant A envia factura y log queda con tenant_id A.
- Tenant B envia factura y log queda con tenant_id B.
- Tenant A no puede ver historial de factura de Tenant B.
- Tenant A no puede reenviar factura de Tenant B.
- Webhook Resend actualiza el invoice_email_log correcto por provider_message_id.
- GET /erp/sri/email/logs solo muestra logs del tenant autenticado.
- POST /erp/sri/email/settings/test no expone secretos.
```

### 67.3 Produccion

```text
- Verificar dominio insightvisionia.cloud en Resend.
- Confirmar DNS SPF/DKIM/DMARC.
- Confirmar que soporte@insightvisionia.cloud esta habilitado como remitente.
- Probar envio real a correo controlado.
- Probar adjunto PDF.
- Probar adjunto XML.
- Probar webhook con firma valida.
- Probar webhook con firma invalida.
```

---

## 68. Prompt para Codex con GPT-5.5: cerrar correos de facturacion Resend

Copiar y pegar en Codex dentro del proyecto abierto en WebStorm / IntelliJ IDEA:

```text
Necesito que cierres completamente el modulo de correos de facturacion con Resend en este proyecto Spring Boot 3.5.7 Java 17 con Flyway y PostgreSQL.

Contexto real detectado:
- Ya existe modulo SRI/RIDE.
- Ya existen TaxDocumentEntity, SriAutomationService, SriDocumentService, SriRideService, RideStorageService.
- Ya existe EmailService.
- Ya existe ResendEmailClient.
- Ya existe TenantEmailSettingsEntity.
- Ya existen campos de resumen en tax_documents: ride_email_status, ride_email_error, ride_email_attempts, ride_email_next_retry_at, ride_email_sent_at.
- Ya existe Resend + SMTP fallback, pero falta cerrarlo para produccion.
- El sistema es multi-tenant y todas las operaciones deben filtrar por tenant_id.

Correo oficial obligatorio:
soporte@insightvisionia.cloud

Proveedor principal:
Resend

Objetivos:
1. Alinear configuracion global de Resend para que el from y reply-to por defecto sean soporte@insightvisionia.cloud.
2. Agregar RESEND_REPLY_TO y RESEND_WEBHOOK_SECRET a configuracion y ejemplos.
3. Extender ResendEmailClient para devolver resultado estructurado con provider_message_id.
4. Guardar el message_id de Resend.
5. Crear historial detallado por intento de envio en invoice_email_logs.
6. Crear soporte de webhook Resend con validacion de firma.
7. Crear plantillas HTML de correo versionadas por tenant/global.
8. Refactorizar SriAutomationService para dejar de usar cuerpo hardcodeado y usar EmailTemplateService.
9. Crear endpoints de envio manual, reenvio, historial, logs, settings, test y webhook.
10. Exponer metricas de correos para dashboard/reportes.
11. Mantener compatibilidad con el flujo SRI/RIDE actual.
12. No romper endpoints existentes.
13. No duplicar tablas existentes.
14. No exponer secretos al frontend.
15. Agregar pruebas unitarias e integracion para aislamiento multi-tenant.

Migraciones Flyway PostgreSQL requeridas:
- invoice_email_logs
- resend_webhook_events
- columnas faltantes en tax_documents:
  - ride_email_last_attempt_at
  - ride_email_provider
  - ride_email_message_id
  - ride_email_template_code
- email_templates

Reglas de base de datos:
- invoice_email_logs debe tener tenant_id, tax_document_id, provider, provider_message_id, from_email, from_name, reply_to_email, to_email, subject, template_code, status, error_code, error_message, retryable, attachment_count, attachment_total_bytes, sent_at, delivered_at, bounced_at, complained_at, opened_at, clicked_at, created_at, updated_at.
- resend_webhook_events debe guardar payload_json y provider_message_id.
- Toda consulta de logs debe filtrar por tenant_id.
- Crear indices por tenant_id, tax_document_id, status, created_at y provider_message_id.

Endpoints a crear:
POST /erp/sri/documents/{id}/email/send
POST /erp/sri/documents/{id}/email/resend
GET /erp/sri/documents/{id}/email/history
GET /erp/sri/email/logs
GET /erp/sri/email/settings
PUT /erp/sri/email/settings
POST /erp/sri/email/settings/test
GET /erp/sri/email/statistics
POST /integrations/resend/webhook

Reglas de endpoints:
- Los endpoints internos usan JWT y tenant del usuario autenticado.
- Nunca permitir consultar logs de otro tenant.
- Nunca aceptar tenantId libre por query param para saltarse seguridad.
- Webhook Resend no usa JWT, pero debe validar firma con RESEND_WEBHOOK_SECRET.
- No devolver API keys ni secretos.
- Reenvio manual debe validar rol/permisos.

Plantillas requeridas:
- SRI_INVOICE_ISSUED
- SRI_INVOICE_RESEND
- SRI_INVOICE_SEND_FAILED_ADMIN

Variables de plantilla:
- issuerName
- issuerRuc
- customerName
- customerEmail
- documentNumber
- issueDate
- totalAmount
- authorizationNumber
- supportEmail

Cambios especificos:
1. Reemplaza defaults antiguos de correo por soporte@insightvisionia.cloud.
2. Actualiza application.yml, application-example.yml y .env.example.
3. Modifica SriProperties para incluir replyTo, webhookSecret y limites de adjuntos.
4. Modifica ResendEmailClient para parsear id devuelto por Resend.
5. Modifica EmailService para devolver EmailSendResult estructurado.
6. Modifica SriAutomationService para registrar cada intento en invoice_email_logs.
7. Actualiza tax_documents con estado resumen y message_id.
8. Implementa ResendWebhookController y ResendWebhookService.
9. Implementa EmailTemplateService.
10. Implementa InvoiceEmailLogService.
11. Implementa TenantEmailSettingsService si no existe.
12. Implementa endpoints REST indicados.
13. Agrega validaciones de adjuntos, destinatario y permisos.
14. Agrega pruebas.

Criterio de aceptacion:
- El proyecto compila.
- Flyway corre sin errores.
- Se puede enviar una factura por correo desde soporte@insightvisionia.cloud.
- El envio guarda invoice_email_logs.
- El message_id de Resend queda guardado.
- El historial por factura funciona.
- Los logs por tenant funcionan.
- Un tenant no puede ver ni reenviar facturas de otro tenant.
- El webhook de Resend guarda eventos y actualiza estados.
- Dashboard/reportes pueden consultar enviados, fallidos, pendientes y entregados.
- No hay secretos expuestos.
```

---

## 69. Prompt para Opus 4.7: auditoria de correos Resend

Usar despues de que Codex implemente:

```text
Actua como auditor tecnico senior. Revisa el modulo de correos de facturacion con Resend implementado en este proyecto Spring Boot multi-tenant.

Busca especificamente:
1. From/reply-to que no usen soporte@insightvisionia.cloud por defecto.
2. API keys o secretos expuestos en logs, frontend o respuestas.
3. Consultas de invoice_email_logs sin tenant_id.
4. Reenvio de facturas sin validar tenant.
5. Historial de factura visible para otro tenant.
6. Webhook Resend sin validacion de firma.
7. message_id de Resend no persistido.
8. Estados inconsistentes entre invoice_email_logs y tax_documents.
9. Reintentos que puedan duplicar envios sin control.
10. Falta de limites de adjuntos.
11. Falta de rate limit en reenvios.
12. Plantillas HTML inseguras o sin fallback.
13. Falta de pruebas multi-tenant.
14. Riesgos de produccion.

Devuelve hallazgos concretos con archivo, riesgo, impacto y correccion sugerida.
```

---

## 70. Prompt final para Codex despues de auditoria de correos

```text
Aplica las correcciones de auditoria del modulo de correos de facturacion con Resend. No cambies lo que ya funciona. Corrige solamente riesgos reales de seguridad, tenant isolation, Resend, webhooks, logs, reintentos, plantillas, adjuntos, dashboard/reportes y pruebas. Ejecuta pruebas al final y resume archivos modificados.
```

---

## 71. Cierre tecnico

El sistema debe quedar con dos bloques fuertes y conectados:

```text
1. Vision AI multi-tenant:
   recepcion, validacion, almacenamiento, agregacion y consulta de detecciones por negocio, camara, sucursal y periodo.

2. Facturacion + correos Resend:
   envio automatico/manual de facturas, RIDE, XML, historial, reintentos, webhooks, metricas, dashboard y reportes por tenant.
```

Prioridad final:

```text
1. Aislamiento por tenant.
2. Vision AI separado por negocio.
3. Correos de facturacion separados por tenant.
4. Resend configurado con soporte@insightvisionia.cloud.
5. API keys y secretos protegidos.
6. Estadisticas Vision AI para dashboard/reportes.
7. Logs de correos para dashboard/reportes.
8. Webhook Resend para trazabilidad real.
9. Idempotencia y reintentos controlados.
10. Produccion sin mezcla de datos.
```

