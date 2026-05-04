# Cierre Operativo SRI Ambiente 2

## Alcance real

- El flujo SRI del backend ya cubre preparacion XML, firma, recepcion, autorizacion, persistencia y reintentos.
- Este cierre deja el modulo listo para validacion E2E real en ambiente 2 y piloto comercial controlado.
- El runtime principal sigue en MySQL. Los artefactos de `db/supabase/` sirven para preparar un workspace Postgres/Supabase de apoyo, no reemplazan la base transaccional actual.

## Variables requeridas

Configurar en `backend/.env`:

```env
SRI_SIGNATURE_MODE=PKCS12
SRI_PKCS12_PATH=/app/certs/emisor.p12
SRI_PKCS12_PASSWORD=********
SRI_KEY_ALIAS=

SRI_CONNECT_TIMEOUT_MS=5000
SRI_READ_TIMEOUT_MS=15000
SRI_AUTHORIZATION_POLL_DELAY_MS=1500
SRI_AUTO_QUERY_AFTER_RECEPTION=true

SRI_PRODUCTION_RECEPTION_URL=https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline
SRI_PRODUCTION_AUTHORIZATION_URL=https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline
```

Montaje Docker:

- Colocar el certificado en `ops/sri/certs/emisor.p12`.
- `docker-compose.yml` ya monta `ops/sri/certs` dentro de `/app/certs`.

## Configuracion fiscal minima

Antes de emitir en ambiente 2, el tenant debe tener:

- `environmentCode=2`
- `emissionCode=1` salvo que el emisor use otro codigo permitido
- RUC emisor de 13 digitos
- razon social exacta del RUC
- direccion matriz y direccion del establecimiento
- obligacion contable correcta
- establecimiento y punto de emision coherentes con la secuencia
- secuencial habilitado para el tipo documental
- reglas de impuestos activas y consistentes con el XML que se genera

## Datos minimos por documento

- tipo documental SRI soportado
- fecha de emision
- comprador con identificacion valida
- nombre del comprador
- subtotal, impuestos y total cuadrando exactamente
- clave de acceso generada
- XML firmado sin error

## Endpoints operativos nuevos

- `GET /api/erp/sri/documents/{id}/status`
  - resumen operativo del documento, ambiente, intentos, ultimo trace y accion sugerida
- `GET /api/erp/sri/documents/{id}/diagnostics`
  - bloqueo actual, checklist manual y siguiente paso recomendado
- `POST /api/erp/sri/documents/{id}/retry?mode=AUTO`
  - reintento seguro
- `POST /api/erp/sri/documents/{id}/retry?mode=RECEPTION`
  - fuerza nuevo envio a recepcion
- `POST /api/erp/sri/documents/{id}/retry?mode=AUTHORIZATION`
  - reconsulta solo autorizacion
- `GET /api/erp/sri/documents/{id}/transmissions`
  - auditoria resumida
- `GET /api/erp/sri/documents/{id}/transmissions/{transmissionId}`
  - request y response SOAP completos

## Comportamiento operativo endurecido

- `emit()` ya no reenvia recepcion si el documento ya quedo en estado `SENT` o `RECIBIDA`; en ese caso consulta autorizacion.
- Cada transmision ahora guarda:
  - `attempt_number`
  - `trace_id`
  - `transport_error`
  - request SOAP
  - response SOAP
- El reintento `AUTO` primero intenta diagnosticar si conviene consultar autorizacion antes de volver a enviar recepcion.

## Flujo manual de homologacion en ambiente 2

1. Subir `backend/.env` con credenciales y endpoints definitivos.
2. Montar el certificado PKCS12 real en `ops/sri/certs/`.
3. Levantar stack con `docker compose up -d --build`.
4. Configurar el tenant emisor con `environmentCode=2`.
5. Validar un documento real con `POST /api/erp/sri/documents/{id}/validate`.
6. Emitir con `POST /api/erp/sri/documents/{id}/emit`.
7. Consultar `status` y `diagnostics`.
8. Si el estado queda en proceso o hubo error tecnico, usar `retry`.
9. Guardar evidencia de:
   - XML firmado
   - request SOAP
   - response SOAP
   - trace id
   - numero de autorizacion
   - fecha de autorizacion

## Evidencia obligatoria antes de piloto real

- documento autorizado real en ambiente 2
- numero de autorizacion persistido
- XML autorizado persistido
- request/response SOAP recuperables por endpoint
- trace id del envio y de la consulta de autorizacion
- validacion manual de un rechazo y su diagnostico
- validacion manual de un timeout o error tecnico y su reintento

## Escenarios manuales a probar

- recepcion exitosa + autorizacion exitosa
- recepcion exitosa + autorizacion en `PROCESSING`
- error tecnico en recepcion y `retry=AUTO`
- error tecnico en autorizacion y `retry=AUTHORIZATION`
- rechazo por datos fiscales invalidos
- autorizacion manual de contingencia con trazabilidad completa

## Invalida salida a piloto

- certificado no corresponde al RUC emisor
- ambiente del tenant distinto de `2`
- documento sin clave de acceso o XML firmado
- no existe evidencia recuperable de request/response
- documentos rechazados sin causa entendida
- error tecnico repetido sin diagnostico ni plan de operacion
- secuenciales inconsistentes entre emisor, establecimiento y punto de emision
