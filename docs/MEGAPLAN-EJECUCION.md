# MEGAPLAN TOTAL DE EJECUCION — UCACUE ERP PLATFORM

> Documento maestro de trabajo. Generado 2026-04-16.
> Este documento guia toda la evolucion del ERP desde su estado actual hasta plataforma multi-tenant, multi-sucursal, multi-industria de clase mundial para Ecuador y LATAM.

---

## A. DIAGNOSTICO ESTRATEGICO

### A.1 Estado actual

| Capacidad | Estado | Madurez |
|-----------|--------|---------|
| Multi-tenant (tenant_id en todas las entidades) | Operativo | Media |
| Auth JWT + Firebase + TOTP 2FA | Operativo | Alta |
| POS con carrito, pagos split, caja registradora | Operativo | Alta |
| Inventario basico (movimientos, alertas, stock) | Operativo | Media |
| Contabilidad (plan de cuentas, diario, asientos) | Operativo | Media |
| SRI Ecuador (facturacion electronica, XML firmado, SOAP) | Operativo | Alta |
| Compras (ordenes, recepcion, pago) | Operativo | Media |
| CxC / CxP (basico) | Operativo | Baja |
| Loyalty (puntos, ledger) | Operativo | Media |
| Vision AI (camaras, deteccion, metricas) | Operativo | Media |
| ML (demand forecast, preparation time) | Operativo | Baja |
| Dashboard ejecutivo | Operativo | Media |
| Reportes y exportacion (CSV/Excel/PDF) | Operativo | Media |
| Planes comerciales (START/PRO/VISION_AI) | Operativo | Media |
| Feature flags por tenant | Operativo | Media |
| Notificaciones (FCM, WebSocket, Email) | Operativo | Media |
| Auditoria (login, entidades, acciones) | Operativo | Media |
| Design system (Aurora green, Cormorant/DM Sans, warm beige) | Operativo | Alta |

### A.2 Brechas criticas

1. **Party Model unificado** — No existe. Clientes/proveedores dispersos.
2. **Permisos granulares** — Solo role-based (ADMIN, BUYER, CASHIER, CUSTOMER). Falta por accion.
3. **Workflows/aprobaciones** — No existe motor configurable.
4. **CRM** — No existe.
5. **Presupuestos** — No existe.
6. **Activos fijos** — No existe.
7. **Conciliacion bancaria** — No existe.
8. **Sucursales (BranchEntity)** — No existe como entidad.
9. **Bodegas (WarehouseEntity)** — Solo un string en ProductEntity.
10. **Vertical Restaurante completa** — Solo POS. Falta mesas, cocina, recetas, reservaciones.
11. **Vertical Educacion** — No existe.
12. **Vertical Service Desk** — No existe.
13. **RRHH** — No existe.
14. **Pricing engine** — No existe. Precio fijo en ProductEntity.
15. **Retenciones Ecuador** — No implementado.
16. **Devoluciones formales** — No existe.

### A.3 Que hace vendible en Ecuador

1. SRI integrado (ya existe)
2. Multi-RUC/multi-sucursal (a construir)
3. Retenciones IR + IVA (a construir)
4. ATS (a construir)
5. Plan de cuentas NIIF Ecuador (a parametrizar)
6. Nomina basica IESS (a construir)

---

## B. ARQUITECTURA OBJETIVO

### Capas

```
PORTALES/UI  (Angular 21 + Design System Aurora + PWA)
    |
API GATEWAY  (Spring Boot + JWT + Rate Limit + Tenant Context)
    |
DOMAIN SERVICES  (Platform | Masters | Financial | Commercial | Operations | Verticals | AI | Growth)
    |
INFRASTRUCTURE  (PostgreSQL + Redis + Firebase + SRI SOAP + S3)
```

### Dominios y dependencias

```
Platform Core --> (todos dependen de este)
    |
Masters Core --> Financial Core
    |                |
Commercial Core <----+
    |                |
Operations Core <----+
    |
Verticals <-- (Restaurant, Education, ServiceDesk, RRHH)
    |
AI Engine <-- (transversal)
    |
Growth Platform <-- (BI, portales, onboarding)
```

### Principios de integracion

1. Event-driven entre modulos (venta completa -> asiento contable)
2. API interna por dominio (*DomainService)
3. Configuracion sobre codigo (impuestos, cuentas, secuencias configurables por tenant)
4. Tenant isolation en todas las queries (TenantScoped)
5. Cada modulo sigue layered architecture: domain/ application/ infrastructure/ interfaces/

---

## C. NUCLEO CANONICO REFINADO

### Reglas de reutilizacion

- ProductEntity existente = Item canonico. Evolucionar, NO duplicar.
- Customer = PartyRole(CUSTOMER) sobre PartyEntity
- Supplier = PartyRole(SUPPLIER) sobre PartyEntity
- Employee = PartyRole(EMPLOYEE) sobre PartyEntity
- Student = PartyRole(STUDENT) sobre PartyEntity
- AccountCatalogEntity existente = Account canonico
- OrderEntity = transaccion POS. SalesOrderEntity = venta formal con flujo.

### Entidades nuevas Fase 1

- BranchEntity (sucursal)
- WarehouseEntity (bodega)
- PartyEntity, PartyIdentifierEntity, PartyAddressEntity, PartyContactEntity, PartyRoleEntity
- ApprovalDefinitionEntity, ApprovalStepEntity, ApprovalInstanceEntity, ApprovalActionEntity
- UserPermissionOverrideEntity (permisos granulares extra por usuario)

---

## D. MATRIZ MAESTRA DE MODULOS

### D.1 Branch (Sucursales)
- **Objetivo:** Multi-sucursal real por tenant
- **Entidades:** BranchEntity
- **Procesos:** CRUD, asignacion de usuarios, config por sucursal
- **Integraciones:** Todos los modulos filtran por branch. Secuencias SRI por sucursal.
- **Permisos:** branch:create, branch:read, branch:update, branch:delete, branch:switch
- **Impacto contable:** Centro de costos por sucursal
- **Impacto tributario:** Punto de emision SRI por sucursal (establecimiento)

### D.2 Warehouse (Bodegas)
- **Objetivo:** Ubicaciones fisicas de almacenamiento
- **Entidades:** WarehouseEntity
- **Procesos:** CRUD, asignacion a sucursal, transferencias
- **Integraciones:** Inventario, compras (recepcion), POS (despacho)
- **Permisos:** warehouse:create, warehouse:read, warehouse:update, warehouse:manage_stock

### D.3 Party Model
- **Objetivo:** Modelo unificado de personas y organizaciones
- **Entidades:** Party, PartyIdentifier, PartyAddress, PartyContact, PartyRole
- **Procesos:** CRUD persona/org -> asignar identificadores -> asignar roles
- **Integraciones:** CRM, ventas, compras, contabilidad, educacion, RRHH
- **Impacto tributario:** RUC/cedula para facturacion SRI

### D.4 Permission Engine V2
- **Objetivo:** Control granular por accion, modulo, sucursal
- **Modelo:** permission = module:action (purchases:approve, pos:void_sale)
- **Evaluacion:** Dinamica en runtime, cache en memoria

### D.5 Approval Engine
- **Objetivo:** Flujos configurables por tenant
- **Entidades:** ApprovalDefinition, ApprovalStep, ApprovalInstance, ApprovalAction
- **Integraciones:** Compras, notas de credito, ajustes inventario, descuentos

### D.6 CxC Enhanced
- **Estados:** OPEN -> PARTIALLY_PAID -> PROMISED -> DISPUTED -> OVERDUE -> LEGAL -> WRITTEN_OFF -> PAID -> CANCELLED
- **Entidades:** ReceivableApplication, PromiseToPay, DunningRule, DunningAction, CollectionLog, InterestCharge, DisputeCase

### D.7 CxP Enhanced
- **Estados:** OPEN -> PARTIALLY_PAID -> APPROVAL_PENDING -> DISPUTED -> DUE -> PAID -> CANCELLED

### D.8 Budget
- **Entidades:** Budget, BudgetVersion, BudgetLine
- **Control:** Por cuenta x periodo x centro de costo

### D.9 Bank & Reconciliation
- **Entidades:** BankAccount, BankTransaction, BankReconciliation

### D.10 Fixed Assets
- **Entidades:** FixedAsset, FixedAssetDepreciation
- **Metodos:** Linea recta, saldos decrecientes

### D.11 Withholdings (Retenciones Ecuador)
- **Entidades:** WithholdingRule, Withholding, WithholdingLine
- **Tributario:** Retencion IR + IVA, comprobante electronico SRI

### D.12 CRM
- **Entidades:** Lead, Opportunity, PipelineStage, Activity
- **Estados Lead:** NEW -> QUALIFIED -> CONTACTED -> PROPOSAL -> NEGOTIATION -> WON -> LOST

### D.13 Quotations
- **Entidades:** SalesQuotation, SalesQuotationLine, SalesQuotationVersion
- **Estados:** DRAFT -> SENT -> APPROVED -> ACCEPTED -> REJECTED -> EXPIRED

### D.14 Sales Orders
- **Entidades:** SalesOrder, SalesOrderLine
- **Estados:** DRAFT -> CONFIRMED -> PARTIALLY_FULFILLED -> FULFILLED -> INVOICED

### D.15 Pricing Engine
- **Entidades:** PriceList, PriceListLine, PriceRule, Promotion, Coupon
- **Logica:** Evaluar listas -> aplicar reglas -> mejor precio

### D.16 Returns
- **Entidades:** Return, ReturnLine
- **Impacto:** Nota de credito SRI, reingreso inventario

### D.17 Enhanced Inventory
- **Entidades:** StockBalance (producto x bodega x lote), Lot, Serial, Transfer, TransferLine, CountSession, CountLine, ValuationLayer, ReplenishmentRule
- **Costeo:** Promedio ponderado (default EC), FIFO opcional

### D.18 Restaurant Vertical
- **Entidades:** FloorPlan, DiningTable, Reservation, WaitlistEntry, KitchenStation, KitchenTicket, KitchenTicketItem, Recipe, RecipeIngredient, Modifier, Combo

### D.19 Education Vertical
- **Entidades:** AcademicProgram, AcademicCourse, AcademicPeriod, Student (PartyRole), Enrollment, GradeComponent, GradeRecord, AttendanceRecord, TuitionAccount, PaymentPlan, Scholarship

### D.20 Service Desk
- **Entidades:** Ticket, SLA, KnowledgeArticle, WorkOrder, MaintenancePlan

### D.21 RRHH Basico
- **Entidades:** Employee (PartyRole), Department, Position, Contract, PayrollPeriod, Payslip

---

## E. ROADMAP MAESTRO POR FASES

### FASE 1 — CIMIENTOS DE PLATAFORMA (8-10 semanas)

#### 1.1 Branch & Warehouse (2 sem)
- BranchEntity, WarehouseEntity
- CRUD backend + frontend
- Migracion Flyway V12
- Filtros por sucursal en UI
- **DoD:** CRUD funcional, tests, UI integrada

#### 1.2 Permission Engine V2 (2 sem)
- Permisos granulares module:action
- UI asignacion de permisos
- Guard backend dinamico
- **DoD:** Permisos evaluados en runtime, UI de config

#### 1.3 Party Model (2 sem)
- Party + Identifier + Address + Contact + Role
- Migracion de datos existentes
- CRUD frontend
- **DoD:** Party model operativo, datos migrados

#### 1.4 Approval Engine (1 sem)
- Definition + Instance + Action
- UI de configuracion de flujos
- Integracion con compras
- **DoD:** Flujo de aprobacion en compras funcionando

#### 1.5 Sequence Engine Enhanced (1 sem)
- Secuencias por sucursal + punto emision
- UI de configuracion
- Integracion SRI
- **DoD:** Secuencias generando numeros correctos por sucursal

### FASE 2 — FINANCIAL CORE (8-10 semanas)

#### 2.1 CxC V2 + CxP V2 (3 sem)
#### 2.2 Bank & Reconciliation (2 sem)
#### 2.3 Budget (2 sem)
#### 2.4 Fixed Assets (1 sem)
#### 2.5 Withholdings + ATS (2 sem)

### FASE 3 — COMMERCIAL CORE (6-8 semanas)

#### 3.1 CRM (2 sem)
#### 3.2 Quotations (1.5 sem)
#### 3.3 Sales Orders (1.5 sem)
#### 3.4 Pricing Engine (1.5 sem)
#### 3.5 Returns (1 sem)
#### 3.6 Customer 360 (0.5 sem)

### FASE 4 — OPERATIONS ENHANCED (4-6 semanas)

#### 4.1 StockBalance multi-bodega (1.5 sem)
#### 4.2 Transfers (1 sem)
#### 4.3 Count Sessions (1 sem)
#### 4.4 Purchase Requisitions (1 sem)
#### 4.5 Supplier Evaluation (0.5 sem)
#### 4.6 Replenishment Rules (0.5 sem)

### FASE 5 — VERTICALES (8-12 semanas)

#### 5.1 Restaurant (4 sem)
#### 5.2 Education (4 sem)
#### 5.3 Service Desk (2 sem)
#### 5.4 RRHH Basico (3 sem)

### FASE 6 — IA + CRECIMIENTO (6-8 semanas)

#### 6.1 IA Documental (2 sem)
#### 6.2 IA Comercial (1.5 sem)
#### 6.3 IA Financiera (1 sem)
#### 6.4 IA Operativa (1 sem)
#### 6.5 IA Restaurant + Education (1 sem)
#### 6.6 Vision YOLO (1.5 sem)
#### 6.7 BI + Dashboards (1 sem)
#### 6.8 Portales + Onboarding (1 sem)

---

## F. BACKLOG DETALLADO — FASE 1

### Epic: Branch & Warehouse

**Features:**
- F1.1.1: BranchEntity backend (entity, repo, service, controller, DTO)
- F1.1.2: WarehouseEntity backend (entity, repo, service, controller, DTO)
- F1.1.3: Migracion Flyway V12 (tablas erp_branches, erp_warehouses)
- F1.1.4: Frontend modulo sucursales (CRUD, listado, formulario)
- F1.1.5: Frontend modulo bodegas (CRUD, listado, formulario)
- F1.1.6: Branch selector en shell header
- F1.1.7: Filtrado por branch_id en queries existentes

**Historias tecnicas:**
- HT1: Crear BranchEntity con TenantScoped, indices, validaciones
- HT2: Crear WarehouseEntity con FK a Branch, TenantScoped
- HT3: BranchService con CRUD + validacion de unicidad code por tenant
- HT4: WarehouseService con CRUD + validacion FK branch
- HT5: BranchController REST endpoints
- HT6: WarehouseController REST endpoints
- HT7: Flyway V12 DDL + seed datos iniciales
- HT8: Agregar branch_id y warehouse_id a erp_tenants como defaults

**Historias UI:**
- HU1: Pantalla listado de sucursales (tabla paginada, filtros, empty state)
- HU2: Modal crear/editar sucursal (formulario reactivo, validaciones)
- HU3: Pantalla listado de bodegas (tabla paginada, filtros por sucursal)
- HU4: Modal crear/editar bodega (formulario reactivo, FK sucursal)
- HU5: Branch selector en header del shell (dropdown con sucursales)
- HU6: Indicador visual de sucursal activa

### Epic: Permission Engine V2

**Features:**
- F1.2.1: Catalogo de permisos por modulo (seed data)
- F1.2.2: Asignacion de permisos a roles customizables
- F1.2.3: Override de permisos por usuario
- F1.2.4: Evaluador de permisos en runtime (cache Caffeine)
- F1.2.5: @RequirePermission annotation + AOP aspect
- F1.2.6: Frontend UI gestion de roles y permisos

### Epic: Party Model

**Features:**
- F1.3.1: PartyEntity + PartyIdentifierEntity
- F1.3.2: PartyAddressEntity + PartyContactEntity
- F1.3.3: PartyRoleEntity (CUSTOMER, SUPPLIER, EMPLOYEE, STUDENT, GUARDIAN)
- F1.3.4: Migracion de datos (users con rol CUSTOMER -> Party)
- F1.3.5: Frontend CRUD de contactos/partes
- F1.3.6: Busqueda unificada de partes

### Epic: Approval Engine

**Features:**
- F1.4.1: ApprovalDefinitionEntity + ApprovalStepEntity
- F1.4.2: ApprovalInstanceEntity + ApprovalActionEntity
- F1.4.3: ApprovalService (crear instancia, avanzar, aprobar/rechazar)
- F1.4.4: Integracion con PurchaseService
- F1.4.5: Frontend bandeja de aprobaciones

### Epic: Sequence Engine

**Features:**
- F1.5.1: Evolucionar DocumentSequenceEntity (branch_id, prefix, padding)
- F1.5.2: SequenceService con generacion thread-safe
- F1.5.3: UI configuracion de secuencias por sucursal
- F1.5.4: Integracion con SRI (establecimiento = branch.sriCode)

---

## G. MATRIZ DE PERMISOS Y GOBIERNO

### Estructura de permisos

```
Formato: {modulo}:{accion}

Ejemplos:
- pos:create_order
- pos:void_sale
- pos:open_register
- pos:close_register
- inventory:view
- inventory:create_movement
- inventory:adjust_stock
- purchases:create
- purchases:approve
- purchases:receive
- purchases:pay
- accounting:view_entries
- accounting:create_entry
- accounting:post_entry
- accounting:reverse_entry
- sri:emit_document
- sri:configure
- branches:create
- branches:update
- branches:switch
- warehouses:create
- warehouses:manage_stock
- parties:create
- parties:update
- parties:delete
- approvals:approve
- approvals:reject
- approvals:configure
- reports:view
- reports:export
- settings:manage
- users:create
- users:update
- users:assign_permissions
```

### Operaciones criticas (requieren MFA o doble confirmacion)

- accounting:post_entry (asientos irreversibles)
- accounting:reverse_entry
- sri:emit_document
- purchases:approve (montos > umbral configurable)
- pos:void_sale
- users:assign_permissions
- settings:manage (cambios criticos)

### Segregacion de funciones

- Quien crea una compra NO puede aprobarla
- Quien registra un pago NO puede conciliar
- Quien crea un asiento NO puede postearlo (si el tenant lo configura)

---

## H. MAPA DE IMPACTOS CONTABLES

### Por tipo de transaccion

#### Venta POS (OrderEntity -> SaleEntity)
```
DEBITO:  Caja/Banco (segun metodo pago)     [monto total]
CREDITO: Ingresos por ventas                 [subtotal]
CREDITO: IVA por pagar                       [impuesto]
```
Configuracion: Cuenta de ingresos por categoria de producto. Cuenta de IVA por regla fiscal.

#### Compra (PurchaseEntity)
```
DEBITO:  Inventario/Gasto (segun tipo)       [subtotal]
DEBITO:  IVA credito tributario              [impuesto]
CREDITO: Cuentas por pagar                   [total]
```

#### Pago a proveedor
```
DEBITO:  Cuentas por pagar                   [monto pagado]
CREDITO: Banco/Caja                          [monto pagado]
```

#### Cobro a cliente
```
DEBITO:  Banco/Caja                          [monto cobrado]
CREDITO: Cuentas por cobrar                  [monto cobrado]
```

#### Retencion en compra (Ecuador)
```
DEBITO:  Cuentas por pagar                   [monto retenido]
CREDITO: Retenciones por pagar (IR)          [valor retencion IR]
CREDITO: Retenciones por pagar (IVA)         [valor retencion IVA]
```

#### Depreciacion activo fijo
```
DEBITO:  Gasto depreciacion                  [cuota mensual]
CREDITO: Depreciacion acumulada              [cuota mensual]
```

#### Ajuste de inventario (positivo)
```
DEBITO:  Inventario                          [costo]
CREDITO: Ajustes de inventario               [costo]
```

#### Devolucion en venta
```
DEBITO:  Devoluciones en ventas              [subtotal]
DEBITO:  IVA por pagar                       [impuesto]
CREDITO: Caja/Banco                          [total]
```

Todas las cuentas deben ser configurables por tenant via AccountMappingEntity (source_transaction_type -> debit_account_id, credit_account_id).

---

## I. MATRIZ DE REPORTES Y DASHBOARDS

### Por modulo

| Modulo | Reportes | KPIs | Dashboard |
|--------|----------|------|-----------|
| POS | Ventas por periodo, por cajero, por metodo pago, Z-Report | Ticket promedio, ventas/hora, conversion | Ventas en tiempo real |
| Inventario | Kardex, stock valorizado, movimientos, alertas | Rotacion, dias de stock, quiebre | Stock critico |
| Compras | Ordenes por estado, por proveedor, pendientes | Tiempo de entrega, cumplimiento | Pipeline compras |
| Contabilidad | Balance general, estado de resultados, mayor, balance de comprobacion | Liquidez, endeudamiento, rentabilidad | Financiero ejecutivo |
| CxC | Antiguedad de cartera, cartera vencida, cobros por periodo | DSO, % mora, recuperacion | Cartera |
| CxP | Obligaciones pendientes, calendario pagos | DPO, flujo caja proyectado | Pagos |
| SRI | Documentos emitidos, pendientes, rechazados | % autorizacion, errores frecuentes | Estado SRI |
| CRM | Pipeline, leads por fuente, conversion | Win rate, ciclo venta, LTV | Pipeline ventas |
| Restaurant | Ventas por mesa, rotacion, ticket promedio, platos top | Rotacion mesas, tiempo servicio | Operacion restaurante |
| Educacion | Matriculados, desercion, rendimiento, mora tuition | Tasa retencion, morosidad | Academico |

---

## J. CAPA DE IA — 31 FUNCIONES

### Prioridad y fase

| # | Funcion | Vertical | Fase | Complejidad | Impacto |
|---|---------|----------|------|------------|---------|
| 1 | OCR documentos | General | 3 | Media | Alto |
| 2 | Clasificacion documental | General | 3 | Media | Medio |
| 3 | Extraccion facturas proveedor | Compras | 2 | Alta | Alto |
| 4 | Extraccion campos ID | General | 3 | Media | Medio |
| 5 | Resumen documental | General | 3 | Baja | Bajo |
| 6 | Validacion adjuntos faltantes | General | 3 | Baja | Medio |
| 7 | Lead scoring | CRM | 3 | Media | Alto |
| 8 | Probabilidad cierre | CRM | 3 | Media | Alto |
| 9 | Churn prediction | Comercial | 3 | Alta | Alto |
| 10 | Next best action | Comercial | 3 | Alta | Alto |
| 11 | Upsell/cross-sell | Comercial | 2 | Media | Alto |
| 12 | Resumen cliente pre-llamada | CRM | 3 | Baja | Medio |
| 13 | Prediccion mora | Finanzas | 2 | Media | Alto |
| 14 | Priorizacion cartera | Finanzas | 2 | Media | Alto |
| 15 | Mejor canal cobranza | Finanzas | 3 | Media | Medio |
| 16 | Anomalias contables | Finanzas | 2 | Alta | Alto |
| 17 | Variaciones presupuestarias | Finanzas | 3 | Media | Medio |
| 18 | Forecast demanda | Operaciones | 1 (ya existe basico) | Alta | Alto |
| 19 | Prediccion quiebre | Operaciones | 2 | Media | Alto |
| 20 | Prediccion sobrestock | Operaciones | 2 | Media | Medio |
| 21 | Sugerencia reabastecimiento | Operaciones | 2 | Media | Alto |
| 22 | Demanda por franja (restaurant) | Restaurant | 2 | Media | Alto |
| 23 | Upsell en caja/mozo | Restaurant | 2 | Media | Alto |
| 24 | Cuellos botella cocina | Restaurant | 3 | Alta | Medio |
| 25 | Ticket riesgo SLA | Restaurant | 3 | Media | Medio |
| 26 | Riesgo academico | Educacion | 3 | Media | Alto |
| 27 | Riesgo desercion | Educacion | 3 | Alta | Alto |
| 28 | Alerta temprana asistencia/notas/mora | Educacion | 3 | Media | Alto |
| 29 | Deteccion aforo (YOLO) | Vision | 1 (ya existe) | Alta | Alto |
| 30 | Clasificacion visual productos | Vision | 3 | Alta | Medio |
| 31 | Deteccion incidencias operativas | Vision | 3 | Alta | Medio |

---

## K. ESTRATEGIA COMERCIAL

### Planes por vertical

| Plan | Modulos incluidos | Precio sugerido/mes |
|------|------------------|-------------------|
| START | POS, Inventario, Clientes, Reportes basicos, 1 sucursal, 1 bodega | $29-49 |
| PRO | START + Compras, Contabilidad, SRI, CxC/CxP, Dashboard, multi-sucursal, multi-bodega | $79-129 |
| ENTERPRISE | PRO + CRM, Cotizaciones, Ordenes venta, Presupuestos, Activos fijos, Aprobaciones, RRHH | $199-349 |
| RESTAURANT | PRO + Mesas, Cocina, Recetas, Reservaciones, Delivery | $149-249 |
| EDUCATION | PRO + Matriculas, Notas, Asistencia, Tuition, Becas | $199-399 |
| VISION_AI | Cualquier plan + Vision AI, Predictions, Footfall | +$49-99 |

### Que vender primero en Ecuador

1. **Restaurantes/bares** — Ya tienen POS. Agregar cocina y mesas = venta inmediata.
2. **Retail pequeno** — POS + inventario + SRI = necesidad basica cubierta.
3. **Empresas comerciales** — Compras + ventas + contabilidad + SRI.
4. **Colegios** — Matriculas + cobros + SRI = alta demanda.

---

## L. RIESGOS DE EJECUCION

### Tecnicos
- Migraciones Flyway fallando en produccion (mitigar: test en staging)
- Performance con muchos tenants (mitigar: indices compuestos, cache)
- Complejidad de party model (mitigar: migracion incremental)

### Funcionales
- Reglas tributarias cambiantes en Ecuador (mitigar: parametrizacion total)
- Clientes pidiendo features antes de que el core este solido (mitigar: roadmap firme)

### Producto
- Vender demasiado barato y no cubrir costos (mitigar: pricing por valor)
- Vender demasiado grande y no entregar (mitigar: MVP por vertical)

### UX
- Sobrecarga de opciones en UI (mitigar: progressive disclosure, feature flags)
- Inconsistencia visual entre modulos (mitigar: shared components, design system estricto)

---

## M. DEFINITION OF DONE GLOBAL

Un modulo se considera DONE cuando:

1. **Backend:** Entity + Repository + Service + Controller + DTOs creados
2. **Base de datos:** Migracion Flyway ejecutable y reversible
3. **Permisos:** Acciones del modulo registradas en catalogo de permisos
4. **Auditoria:** Operaciones sensibles registran AuditLog
5. **Frontend:** Pantallas con loading state, empty state, error state
6. **Validaciones:** Frontend (reactive forms) + Backend (jakarta.validation)
7. **Impacto contable:** Si aplica, asientos automaticos configurados
8. **Impacto tributario:** Si aplica, documentos SRI generados
9. **Tenant isolation:** Todas las queries filtran por tenant_id
10. **Tests:** Al menos service layer con tests unitarios
11. **API documentada:** Endpoints con request/response claros
12. **Responsive:** Funcional en desktop y tablet minimo

---

## N. REGLAS PARA FUTUROS PROMPTS DE IMPLEMENTACION

### Reglas de contexto
1. Siempre indicar que fase y subfase se esta implementando
2. Referenciar este megaplan como fuente de verdad
3. Indicar entidades existentes que se reutilizan vs nuevas

### Reglas de codigo
4. Seguir layered architecture: domain/ application/ infrastructure/ interfaces/
5. Toda entidad nueva implementa TenantScoped
6. Usar @Getter @Setter @NoArgsConstructor de Lombok (patron existente)
7. Naming: erp_{domain}_{entity} para tablas (ej: erp_branches)
8. Migraciones PostgreSQL en db/migration/postgresql/
9. Frontend: standalone components, signals, lazy loading
10. Frontend: reutilizar shared components (skeleton-loader, empty-state, paginator, form-field)
11. Mantener design system: Aurora green, Cormorant Garamond, DM Sans, warm beige

### Reglas de integracion
12. Cada modulo nuevo agrega su ModuleCode al enum CommercialTypes
13. Cada modulo nuevo registra sus permisos en el catalogo
14. Cada modulo nuevo agrega su ruta en routes.ts con moduleAccessGuard
15. Cada modulo nuevo agrega su entrada en navigationCatalog del SessionContext

### Reglas de UI
16. No romper el design system existente
17. Usar skill frontend-design para decisiones de diseno
18. Toda pantalla tiene 3 estados: loading, empty, error
19. Modales para create/edit, tablas para listados
20. Paginacion server-side en todos los listados
