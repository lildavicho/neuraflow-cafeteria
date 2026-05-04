# Propuesta de refactorizacion y evolucion del Sistema 1

## 1. Reglas obligatorias y correccion del diagrama

- Sistema 1 sigue siendo el ERP principal.
- Frontend de Sistema 1: Angular + TypeScript + Tailwind.
- Backend de Sistema 1: Java + Spring Boot.
- Migraciones: Flyway.
- Base de datos objetivo de Sistema 1: PostgreSQL.
- Sistema 2 es externo: Python + FastAPI + PyTorch + YOLO + OpenCV + PostgreSQL.
- El frontend nunca consume Sistema 2.
- El backend Java consume PostgreSQL y Sistema 2 por HTTP.
- Contabilidad, tributacion y SRI pertenecen exclusivamente a Sistema 1.

Correccion obligatoria del diagrama enviado:

- La flecha `Frontend -> API Sistema 2` no es valida para la arquitectura objetivo y debe eliminarse.
- La unica integracion valida es `Angular -> Spring Boot -> FastAPI`.

## 2. Estado actual del repo y criterio de transicion

Hoy el repo todavia mezcla dos realidades:

- Backend actual en Spring Boot con configuracion MySQL legacy.
- Frontend legacy en React.
- Nuevo `frontend-angular/` en progreso.

La estrategia correcta no es romper lo existente de una vez. La estrategia correcta es:

1. Introducir capas y modulos ERP nuevos dentro de Spring Boot.
2. Levantar Angular por features sin tocar el contrato HTTP publico innecesariamente.
3. Migrar el source of truth transaccional a PostgreSQL en una fase dedicada.
4. Mantener Vision AI desacoplado como integracion HTTP externa.

## 3. Estructura objetivo del backend Spring Boot

```text
backend/src/main/java/com/ucacue/bar/
  erp/
    shared/
      domain/
      exception/
    commercial/
      application/
      domain/
      infrastructure/
        persistence/
          entity/
          repository/
      interfaces/
        http/
    accounting/
      application/
      domain/
      infrastructure/
        persistence/
          entity/
          repository/
      interfaces/
        http/
    vision/
      application/
      domain/
      infrastructure/
        http/
      interfaces/
        http/
    sales/
      application/
      domain/
      infrastructure/
      interfaces/
    orders/
    inventory/
    purchases/
    customers/
    reports/
    dashboard/
    taxation/
    sri/
```

### Criterios de capa

- `domain`: reglas puras del negocio, enums, agregados y contratos.
- `application`: casos de uso y orquestacion.
- `infrastructure.persistence`: JPA, repositorios y adaptadores a BD.
- `infrastructure.http`: clientes salientes a Sistema 2.
- `interfaces.http`: controladores REST del ERP.

### Modulos ERP obligatorios

- POS
- inventario
- compras
- clientes
- ordenes
- caja
- reportes
- dashboard ejecutivo
- contabilidad
- tributacion
- SRI Ecuador
- insights
- comercial por tenant y plan

## 4. Estructura objetivo del frontend Angular

Se sigue la skill `angular-architecture`: scope determines structure.

```text
frontend-angular/src/app/
  core/
    models/
    services/
    guards/
    auth-token.interceptor.ts
    tenant-context.interceptor.ts
    api-base.ts
  features/
    auth/
      login/
        login.ts
      register/
        register.ts
      forgot-password/
        forgot-password.ts
    shell/
      shell.ts
    pos/
      pos.ts
    inventory/
      inventory.ts
    customers/
      customers.ts
    reports/
      reports.ts
    executive-dashboard/
      executive-dashboard.ts
    purchases/
      purchases.ts
    insights/
      insights.ts
    accounting/
      accounting.ts
    sri/
      sri.ts
    vision-ai/
      vision-ai.ts
    shared/
      components/
        module-page.ts
        module-pill.ts
  app.ts
  app.config.ts
  routes.ts
```

### Regla de modularidad frontend

- Si un componente lo usa una sola feature, vive dentro de esa feature.
- Si lo usan dos o mas features, va a `features/shared/components`.
- Guards, contexto, interceptores y estado transversal van en `core/`.

## 5. Modulos de dominio del ERP

### Core transaccional

- `orders`: estados claros `PENDING -> CONFIRMED -> PREPARING -> READY -> DELIVERED/COMPLETED -> CANCELLED`
- `inventory`: reserva, commit y release de stock
- `sales`: cierre auditable y source of truth de metricas
- `cash`: apertura, movimientos y cierre
- `customers`: identidad comercial y tributaria base
- `purchases`: ingreso inventariable y cuentas por pagar

### Core analitico

- `reports`: reportes basicos y exportables
- `dashboard`: KPIs ejecutivos
- `insights`: recomendaciones accionables construidas sobre ventas cerradas

### Core fiscal

- `accounting`: catalogo, asientos, cartera, comprobantes
- `taxation`: impuestos configurables y reglas fiscales
- `sri`: documentos tributarios, secuenciales, validaciones y estados

### Core comercial

- `commercial`: tenant, plan, modulos habilitados, feature flags

### Integraciones

- `vision`: cliente HTTP a Sistema 2, fallbacks y transformacion al lenguaje del ERP

## 6. Contratos HTTP entre Sistema 1 y Sistema 2

Base URL configurable:

- `integrations.vision-ai.base-url`

Timeouts recomendados:

- connect timeout: 2s
- read timeout: 5s

Estrategia de errores:

- si FastAPI responde 4xx/5xx: devolver respuesta degradada desde Spring Boot
- si FastAPI no responde: usar ultimo snapshot en cache y marcar `source=STALE_CACHE`
- si no existe cache: devolver `source=UNAVAILABLE` y colecciones vacias

### Contratos salientes Spring Boot -> FastAPI

#### 1. Afluencia

`POST /api/v1/traffic/footfall/query`

Request:

```json
{
  "tenantCode": "default",
  "from": "2026-03-01T00:00:00",
  "to": "2026-03-07T23:59:59",
  "granularity": "HOUR",
  "cameraCodes": ["cam-01"]
}
```

Response:

```json
{
  "totalPeople": 1842,
  "series": [
    { "bucketStart": "2026-03-01T09:00:00", "peopleCount": 123 }
  ],
  "insights": ["El flujo sube entre 12:00 y 14:00."]
}
```

#### 2. Conversion

`POST /api/v1/traffic/conversion/query`

Response:

```json
{
  "totalPeople": 1842,
  "closedSales": 612,
  "conversionRate": 33.22,
  "averageTicket": 6.45,
  "insights": ["El martes convierte mejor que el domingo."]
}
```

#### 3. Horas pico

`POST /api/v1/traffic/peak-hours/query`

Response:

```json
{
  "peakHours": [
    {
      "rank": 1,
      "bucketStart": "2026-03-01T13:00:00",
      "peopleCount": 146,
      "conversionRate": 34.9
    }
  ],
  "insights": ["Refuerza personal en franja 12:00-14:00."]
}
```

#### 4. Insights de trafico

`POST /api/v1/traffic/insights/query`

Response:

```json
{
  "insights": [
    "Aumenta la afluencia despues de las 17:00.",
    "La conversion cae cuando sube la fila."
  ]
}
```

#### 5. Predicciones

`POST /api/v1/predictions/query`

Response:

```json
{
  "predictions": [
    {
      "bucketStart": "2026-03-08T12:00:00",
      "expectedVisitors": 141.0,
      "expectedConversionRate": 31.5,
      "confidenceBand": "MEDIUM"
    }
  ],
  "insights": ["Se espera pico de trafico el viernes."]
}
```

### Contratos expuestos por el ERP al frontend

- `GET /erp/vision-ai/afluencia`
- `GET /erp/vision-ai/conversion`
- `GET /erp/vision-ai/horas-pico`
- `GET /erp/vision-ai/insights`
- `GET /erp/vision-ai/predicciones`

Reglas:

- siempre pasan por Spring Boot
- siempre llevan `X-Tenant-Code`
- siempre validan modulo por plan antes de consultar a Sistema 2

## 7. Modelo base de contabilidad y SRI Ecuador

### Entidades base implementadas/propuestas

- `acc_account_catalog`
- `acc_tax_rules`
- `acc_document_types`
- `acc_document_sequences`
- `acc_journal_entries`
- `acc_journal_entry_lines`
- `acc_receivables`
- `acc_payables`
- `tax_documents`
- `tax_document_tax_lines`

### Relaciones clave

- una venta pagada puede generar:
  - asiento contable
  - documento tributario borrador
  - cuenta por cobrar si no se liquida total
- una compra puede generar:
  - asiento contable
  - cuenta por pagar
  - documento de soporte
- un pago puede cerrar o reducir cartera y generar asiento adicional

### Reglas contables

- los asientos deben balancear debito vs credito
- `source_module + source_type + source_id` enlazan el origen operativo con el impacto contable
- impuestos son configurables por tabla, no hardcodeados
- secuenciales viven en `acc_document_sequences`
- estados tributarios viven en `tax_documents.status`

### Estados tributarios sugeridos

- `DRAFT`
- `PENDING_VALIDATION`
- `READY_TO_SEND`
- `SENT`
- `AUTHORIZED`
- `REJECTED`
- `CANCELLED`

### Separacion obligatoria

- POS vende y cobra
- tributacion decide documento, secuencia y validaciones
- contabilidad registra impacto contable
- SRI es una integracion posterior, no una dependencia para vender

## 8. Feature flags y modulos por plan

### Plan Start

- `pos`
- `inventory`
- `customers`
- `sales`
- `orders`
- `cash`
- `basic-reports`

### Plan Pro

- todo Start
- `purchases`
- `executive-dashboard`
- `insights`
- `accounting`
- `taxation`
- `sri`

### Plan Vision AI

- todo Pro
- `vision-ai`
- `footfall`
- `conversion`
- `peak-hours`
- `traffic-insights`
- `predictions`

### Activacion por tenant

Tablas base:

- `erp_tenants`
- `erp_tenant_plan_subscriptions`
- `erp_tenant_feature_flags`

Regla:

- el plan define el baseline
- los feature flags permiten override puntual por tenant
- el backend valida el modulo antes de servir el endpoint

## 9. Roadmap por fases sin romper el sistema actual

### Fase 0. Estabilizacion de borde

- congelar contratos HTTP del backend actual
- prohibir nuevas llamadas directas del frontend a cualquier servicio externo
- declarar deprecado el frontend React como legacy

### Fase 1. Fundacion ERP moderna

- introducir `erp/commercial`, `erp/accounting` y `erp/vision`
- crear tablas base por Flyway
- exponer plan por tenant y guardas de modulo

### Fase 2. Angular por slices

- mover navegacion a `frontend-angular`
- migrar primero POS, inventario, clientes y reportes
- mantener backend Java como unico BFF

### Fase 3. Dominio fiscal y contable

- integrar ventas pagadas con asientos
- integrar compras con cuentas por pagar
- habilitar documentos tributarios borrador y secuenciales

### Fase 4. Migracion a PostgreSQL

- portar Flyway a PostgreSQL
- recrear migraciones legacy incompatibles con MySQL
- mover datasource Spring Boot a PostgreSQL
- validar consultas, indices y tipos monetarios

### Fase 5. Vision AI comercial

- conectar Spring Boot con FastAPI solo por HTTP
- activar tablero Vision AI por plan
- agregar cache de ultimo resultado y alertas de degradacion

### Fase 6. SRI operativo

- agregar generacion XML
- firma y envio
- recepcion de autorizacion y rechazo
- reintentos y trazabilidad por documento

## 10. Resultado de esta iteracion

Se dejo base implementada en el repo para:

- comercial por tenant y plan en Spring Boot
- estructura contable y tributaria base en Spring Boot
- cliente HTTP saliente hacia Sistema 2 con fallback
- endpoints ERP para Vision AI consumibles por Angular
- estructura Angular por `features/`, `core/` y guardas por modulo

Puntos que siguen siendo transicion y no estado final:

- el backend legacy todavia conserva configuracion MySQL
- el frontend React legacy todavia existe en el repo
- Tailwind debe instalarse sobre `frontend-angular` en la fase visual siguiente
- la migracion total a PostgreSQL debe ejecutarse como fase dedicada, no mezclada con la extraccion funcional
