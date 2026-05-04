# Checklist de piloto controlado

Este documento cierra la fase de estabilizacion del backend para un piloto serio controlado.

Estado actual:

- La suite `mvn test` pasa en `backend/` con pruebas nuevas de SRI, concurrencia transaccional, consistencia de ordenes y enforcement por tenant/plan.
- El flujo SRI interno ya cubre preparacion, firma, recepcion, autorizacion, persistencia de request/response, reintentos y fallback manual.
- Lo que sigue siendo manual y obligatorio es la validacion E2E con credenciales y ambiente oficial del SRI.

## 1. Variables requeridas

Minimas para levantar backend estable:

```bash
DB_URL=
DB_USER=
DB_PASS=
JWT_SECRET=
ALLOWED_ORIGINS=
```

Opcionales segun despliegue:

```bash
SERVER_PORT=8080
CONTEXT_PATH=/api
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=
FIREBASE_ENABLED=false
FIREBASE_PROJECT_ID=
FIREBASE_SERVICE_ACCOUNT_PATH=
LOG_LEVEL=INFO
```

Variables SRI y firma:

```bash
SRI_CONNECT_TIMEOUT_MS=5000
SRI_READ_TIMEOUT_MS=15000
SRI_AUTHORIZATION_POLL_DELAY_MS=1500
SRI_AUTO_QUERY_AFTER_RECEPTION=true

SRI_SIGNATURE_MODE=PKCS12
SRI_PKCS12_PATH=/secure/path/certificado.p12
SRI_PKCS12_PASSWORD=
SRI_KEY_ALIAS=

SRI_TEST_RECEPTION_URL=https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline
SRI_TEST_AUTHORIZATION_URL=https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline
SRI_PRODUCTION_RECEPTION_URL=https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline
SRI_PRODUCTION_AUTHORIZATION_URL=https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline
```

Notas:

- `SRI_SIGNATURE_MODE` soporta `NONE`, `PRESIGNED` y `PKCS12`.
- Para piloto real, usar `PKCS12` o `PRESIGNED`. `NONE` solo sirve para pruebas internas.
- Si `PKCS12` esta activo, `SRI_PKCS12_PATH` y `SRI_PKCS12_PASSWORD` son obligatorios.

## 2. Configuracion fiscal minima por tenant

Antes de emitir, cada tenant debe tener configuracion SRI activa en:

`PUT /api/erp/sri/config`

Header obligatorio:

```http
X-Tenant-Code: <tenant-code>
```

Payload minimo:

```json
{
  "environmentCode": "1",
  "emissionCode": "1",
  "issuerRuc": "0199999999001",
  "issuerLegalName": "Razon Social",
  "issuerTradeName": "Nombre Comercial",
  "matrixAddress": "Direccion matriz",
  "establishmentAddress": "Direccion establecimiento",
  "specialTaxpayer": null,
  "obligatedAccounting": "SI",
  "active": true
}
```

Reglas operativas:

- `environmentCode = 1` para pruebas.
- `environmentCode = 2` para ambiente oficial.
- `issuerRuc` debe tener 13 digitos.
- `issuerLegalName`, `matrixAddress` y `establishmentAddress` no pueden quedar vacios.

## 3. Smoke tests previos al piloto

Ejecutar en `backend/`:

```bash
mvn test
```

Cobertura agregada en estabilizacion:

- `SriDocumentServiceIntegrationTest`
- `AccountingPostingServiceConcurrencyTest`
- `OrderServiceConsistencyIntegrationTest`
- `TenantPlanEnforcementWebMvcTest`

Validaciones minimas antes de abrir piloto:

- Confirmar que toda respuesta devuelve `X-Trace-Id`.
- Confirmar que APIs multi-tenant aceptan `X-Tenant-Code`.
- Confirmar que `Start`, `Pro` y `Vision AI` reciben `403 ACCESS_DENIED` donde aplica.
- Confirmar que dashboard carga con tenant explicito.

## 4. Flujo exacto de prueba SRI en ambiente 1

1. Configurar variables SRI y firma en runtime.
2. Guardar configuracion fiscal del tenant con `environmentCode = 1`.
3. Generar un documento tributario realista desde la operacion del sistema.
4. Validar el documento:

```http
POST /api/erp/sri/documents/{id}/validate
X-Tenant-Code: <tenant-code>
```

Resultado esperado:

- estado `READY_TO_SEND`
- `accessKey` generada
- XML persistido

5. Emitir el documento:

```http
POST /api/erp/sri/documents/{id}/emit
X-Tenant-Code: <tenant-code>
```

Resultado esperado:

- `RECIBIDA` en recepcion
- luego `AUTORIZADO` o `SENT` si aun esta en proceso

6. Revisar trazabilidad:

```http
GET /api/erp/sri/documents/{id}/transmissions
X-Tenant-Code: <tenant-code>
```

Validar que existan request/response persistidos para:

- `RECEPTION`
- `AUTHORIZATION`

7. Si queda en proceso, consultar autorizacion manualmente:

```http
POST /api/erp/sri/documents/{id}/poll-authorization
X-Tenant-Code: <tenant-code>
```

8. Si hubo error de conectividad, reintentar `emit`.

Comportamiento esperado ante fallo del WS:

- respuesta `503 SERVICE_UNAVAILABLE`
- documento permanece en `READY_TO_SEND`
- se persiste la transmision fallida

9. Si el SRI confirma la autorizacion fuera de linea o por gestion manual, registrar fallback:

```http
POST /api/erp/sri/documents/{id}/authorize?authorizationCode=<codigo>
X-Tenant-Code: <tenant-code>
```

Resultado esperado:

- estado `AUTHORIZED`
- `authorizationStatus = MANUAL`

Evidencia a guardar:

- `id` del documento
- `accessKey`
- `authorizationCode`
- payload/request/response de transmisiones
- `X-Trace-Id` de cada llamada

## 5. Paso a ambiente oficial del SRI

No mover a ambiente `2` hasta cumplir todo esto:

- RUC emisor definitivo confirmado
- certificado de firma vigente y probado
- password del PKCS12 validado
- direccion matriz y establecimiento definitivas
- secuenciales oficiales definidos
- usuario responsable del monitoreo del piloto

Secuencia:

1. Cambiar configuracion fiscal del tenant a `environmentCode = 2`.
2. Verificar que el backend este usando certificado real y no uno de prueba.
3. Ejecutar una emision controlada de bajo riesgo.
4. Confirmar recepcion y autorizacion desde el backend.
5. Validar que la trazabilidad persistida coincida con la respuesta oficial.
6. Validar recuperacion ante timeout o indisponibilidad del WS del SRI.

## 6. Casos de fallo que deben probarse manualmente

- Certificado inexistente o password invalido.
- `issuerRuc` invalido o configuracion fiscal incompleta.
- WS de recepcion fuera de servicio.
- Documento rechazado por validacion tributaria.
- Documento `SENT` sin autorizacion inmediata y posterior `poll-authorization`.
- Fallback manual con `authorize`.
- Dos emisiones seguidas del mismo tenant para revisar secuenciales.
- Pago y cancelacion concurrentes sobre la misma orden.

## 7. Criterio de salida a piloto controlado

El backend queda apto para piloto cuando:

- `mvn test` esta verde.
- La configuracion SRI por tenant esta completa.
- El certificado real esta cargado y validado.
- Existe al menos una prueba E2E satisfactoria en ambiente `1`.
- Existe al menos una prueba E2E satisfactoria en ambiente `2`.
- El equipo operativo sabe reintentar `emit`, ejecutar `poll-authorization` y usar `authorize` solo como fallback controlado.
- Cada incidencia puede rastrearse por `X-Trace-Id`.

Si falta la prueba E2E en ambiente `2`, el sistema queda listo para piloto tecnico interno, no para piloto comercial homologado.
