# Checklist de piloto frontend

Este checklist deja el frontend Angular listo para piloto comercial controlado sobre el backend Spring ya endurecido.

## 1. Validacion base antes del piloto

- Ejecutar `npm run build` en `frontend-angular/`.
- Ejecutar `npm run test -- --watch=false` en `frontend-angular/`.
- Confirmar que el frontend apunte al backend correcto en `API_BASE_URL`.
- Confirmar login operativo y token valido en `localStorage`.
- Confirmar que la shell muestra `tenant`, `plan` y `Trace` visible.

## 2. Modulos por plan a verificar

Plan `START`

- `POS`
- `Inventario`
- `Clientes`
- `Reportes basicos`

Plan `PRO`

- todo lo de `START`
- `Compras`
- `Dashboard ejecutivo`
- `Insights`
- `Contabilidad`
- `SRI`

Plan `VISION_AI`

- todo lo de `PRO`
- `Vision AI`

Reglas manuales:

- cambiar tenant desde la shell y confirmar que la navegacion se ajusta al plan real
- intentar entrar a una ruta fuera del plan y validar redireccion segura
- confirmar mensaje visible cuando un modulo devuelve `403 ACCESS_DENIED`

## 3. Flujos criticos a probar manualmente

POS

- crear orden solo reservada
- crear orden y cobrar en efectivo
- crear orden por transferencia y luego confirmar transferencia
- cancelar una orden pendiente
- reembolsar una orden pagada

Inventario

- registrar ingreso manual
- registrar salida manual
- validar stock disponible, reservado y fisico
- validar auditoria de movimientos

Compras

- crear compra con multiples lineas
- recibir compra
- pagar compra
- validar impacto en inventario y CxP

Dashboard ejecutivo

- validar ventas hoy vs ayer
- validar ganancias hoy vs ayer
- validar stock critico
- validar cuentas por cobrar y por pagar
- validar documentos SRI pendientes
- validar alertas e insights sin mocks

Contabilidad

- crear CxC manual
- cobrar CxC
- pagar CxP
- revisar libro diario reciente

SRI

- validar documento
- emitir documento
- consultar autorizacion
- revisar transmisiones
- registrar autorizacion manual solo como fallback

Vision AI

- consultar rango de fechas
- validar afluencia
- validar conversion
- validar horas pico
- validar predicciones
- validar mensaje visible si el modo degradado esta activo

## 4. Rutas criticas

- `/pos`
- `/inventario`
- `/compras`
- `/dashboard-ejecutivo`
- `/contabilidad`
- `/sri`
- `/vision-ai`

## 5. Errores visibles y trazabilidad

El piloto no debe salir si falla alguno de estos puntos:

- cada error backend relevante muestra feedback visible en pantalla
- el frontend conserva y muestra `X-Trace-Id`
- los `403` por plan o rol se entienden sin inspeccionar consola
- los estados vacios son honestos y no muestran datos ficticios
- el cambio de tenant no deja al usuario en una ruta fuera de su plan

## 6. Riesgos pendientes conocidos

- queda una advertencia de build por `@import "tailwindcss"` en `src/styles.scss`
- no reemplaza la validacion E2E real contra SRI oficial
- no reemplaza monitoreo operativo, logs centralizados ni health checks de despliegue
