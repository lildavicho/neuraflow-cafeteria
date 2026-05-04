# PLAN MAESTRO DEFINITIVO V3: ERP MULTI-INDUSTRIA DE CLASE MUNDIAL PARA ECUADOR Y LATAM

> Documento rector final para construir, expandir y gobernar el ERP como plataforma multi-industria.
> Esta V3 deja el plan en formato **ejecutable para equipos humanos y agentes de codigo**.
> El objetivo no es solo definir modulos, sino impedir huecos funcionales, deuda tecnica, duplicacion de entidades, inconsistencias contables, errores de permisos, pantallas aisladas y verticales incompletos.

---

# 0. OBJETIVO FINAL DEL PRODUCTO

Construir un ERP multi-tenant, multi-sucursal, multi-industria y multi-modulo que pueda venderse con solidez a:

- colegios
- universidades e institutos
- restaurantes, bares y cafeterias
- retail y negocios generales
- distribuidoras
- empresas de servicios
- pymes y medianas empresas
- grupos empresariales

El sistema debe ser:

- modular
- auditable
- escalable
- localizable para Ecuador
- exportable a LATAM
- comercialmente paquetizable
- resistente a fallos operativos
- consistente contable y tributariamente
- agradable visualmente
- facil de implantar
- facil de mantener
- facil de extender

---

# 1. REGLA MAESTRA

## Toda mejora debe obedecer esta regla:

**No construir funciones sueltas. Construir capacidades empresariales completas, integradas, auditables, parametrizables, vendibles y listas para operar.**

---

# 2. PRINCIPIOS INNEGOCIABLES

## 2.1 Fuente unica de verdad
No se permiten islas de datos ni logicas paralelas.

## 2.2 Modularidad real
Cada industria activa lo que necesita, pero sobre un mismo core.

## 2.3 Parametrizacion antes que hardcode
Reglas, impuestos, aprobaciones, pricing, workflows, asientos y estados deben vivir en configuracion siempre que sea razonable.

## 2.4 Multi-tenant serio
Aislamiento por:
- datos
- configuraciones
- branding
- numeraciones
- certificados
- impuestos
- planes
- sucursales
- permisos
- modulos habilitados

## 2.5 Auditoria total
Toda accion critica debe responder:
- quien
- cuando
- desde donde
- que cambio
- antes
- despues
- motivo
- entidad
- correlacion tecnica

## 2.6 Impacto financiero consistente
Toda operacion monetaria debe tener criterio contable.

## 2.7 Operacion continua
Los procesos criticos deben soportar:
- cortes de internet
- reintentos
- errores de terceros
- duplicados
- conflictos
- latencia
- caidas parciales

## 2.8 Vendibilidad real
El ERP debe poder cotizarse, implantarse y demostrarse por paquetes.

---

# 3. SCOPE DEFINITIVO DEL PRODUCTO

El ERP queda dividido en 7 capas:

## 3.1 Core de plataforma
- identidad
- seguridad
- sesiones
- permisos
- tenanting
- branding
- feature flags
- aprobaciones
- workflows
- auditoria
- tareas
- comentarios
- archivos
- notificaciones
- observabilidad
- configuraciones globales
- secuencias y series

## 3.2 Core de datos maestros
- party model
- clientes
- proveedores
- contactos
- estudiantes
- empleados
- productos
- servicios
- cuentas contables
- impuestos
- bancos
- sucursales
- bodegas
- cajas
- centros de costo
- dimensiones analiticas

## 3.3 Core financiero
- plan de cuentas
- diario
- periodos
- cuentas por cobrar
- cuentas por pagar
- cobranza
- tesoreria
- bancos
- conciliacion
- presupuestos
- activos fijos
- impuestos
- SRI
- reportes financieros

## 3.4 Core comercial
- CRM
- cotizaciones
- pedidos
- ventas
- POS
- pricing
- loyalty
- gift cards
- devoluciones
- customer 360

## 3.5 Core operativo
- inventario
- WMS
- lotes
- series
- abastecimiento
- compras
- proveedores
- costeo
- transferencias
- conteos
- reabastecimiento

## 3.6 Verticales
- restaurante
- educacion
- retail omnicanal
- RRHH basico
- service desk
- mantenimiento
- proyectos

## 3.7 Plataforma de crecimiento
- onboarding
- migracion de datos
- kits por industria
- semantic layer
- BI
- alertas
- portal cliente
- portal estudiante
- portal empleado

---

# 4. NUCLEO CANONICO DE ENTIDADES

Ningun agente puede crear entidades paralelas si una equivalente ya existe aqui.

## 4.1 Plataforma
- Tenant
- TenantPlan
- TenantFeature
- Branch
- Warehouse
- CashRegister
- CostCenter
- ProfitCenter
- AnalyticDimension
- Attachment
- Comment
- Tag
- AuditLog
- Notification
- NotificationTemplate
- ApprovalDefinition
- ApprovalInstance
- WorkflowDefinition
- WorkflowInstance
- Task
- Sequence
- Series
- Setting
- FeatureFlag

## 4.2 Party model
- Party
- PartyIdentifier
- PartyAddress
- PartyContact
- PartyRole
- PartyRelation

## 4.3 Comercial
- Lead
- Opportunity
- PipelineStage
- Customer
- CustomerCreditProfile
- SalesQuotation
- SalesQuotationVersion
- SalesOrder
- SalesOrderLine
- Delivery
- Return
- ReturnLine
- PriceList
- PriceRule
- Promotion
- Coupon
- GiftCard
- LoyaltyAccount

## 4.4 Operaciones
- Item
- ItemVariant
- ItemCategory
- UnitOfMeasure
- UomConversion
- StockBalance
- StockMovement
- Lot
- Serial
- Transfer
- TransferLine
- ReplenishmentRule
- CountSession
- CountLine
- ValuationLayer

## 4.5 Compras
- Supplier
- SupplierEvaluation
- ApprovedSupplier
- PurchaseRequisition
- PurchaseRequisitionLine
- QuotationRequest
- QuotationRequestLine
- PurchaseOrder
- PurchaseOrderLine
- PurchaseReceipt
- PurchaseReceiptLine
- VendorBill

## 4.6 Finanzas
- Account
- Journal
- FiscalPeriod
- JournalEntry
- JournalEntryLine
- Receivable
- ReceivableApplication
- Payable
- PayableApplication
- PromiseToPay
- DisputeCase
- DunningRule
- DunningAction
- CollectionLog
- InterestCharge
- Budget
- BudgetVersion
- BudgetLine
- FixedAsset
- FixedAssetDepreciation
- BankAccount
- BankTransaction
- BankReconciliation
- TreasuryForecast
- TaxProfile
- TaxRatePeriod
- WithholdingRule
- SriDocumentArchive
- SriErrorCatalog

## 4.7 Restaurante
- FloorPlan
- DiningTable
- Reservation
- WaitlistEntry
- KitchenStation
- KitchenTicket
- KitchenTicketItem
- Recipe
- RecipeIngredient
- Modifier
- Combo
- SelfOrderSession
- DeliveryZone
- Rider

## 4.8 Educacion
- AcademicProgram
- AcademicCourse
- AcademicPeriod
- Student
- GuardianRelation
- Enrollment
- GradeComponent
- GradeRecord
- AttendanceRecord
- Transcript
- TuitionAccount
- PaymentPlan
- Scholarship
- ScholarshipAward
- AdvisorSession
- AdmissionApplication

## 4.9 Servicios y soporte
- Ticket
- SLA
- KnowledgeArticle
- WorkOrder
- MaintenancePlan
- MaintenanceExecution
- Project
- ProjectTask
- Timesheet

---

# 5. MATRIZ MAESTRA DE IMPACTO

Toda historia, modulo, endpoint o automatizacion debe declarar su matriz de impacto.

## 5.1 Estructura obligatoria
Cada feature debe responder:

- entidades afectadas
- estado inicial y final
- impacto en contabilidad
- impacto en impuestos
- impacto en inventario
- impacto en pricing
- impacto en notificaciones
- impacto en archivos
- impacto en reportes
- impacto en BI
- si requiere aprobacion
- si requiere auditoria reforzada
- si soporta offline
- politica de anulacion/cancelacion
- politica de devolucion/reverso

## 5.2 Plantilla obligatoria
```md
### Matriz de impacto - [Feature]

Entidades:
- ...
Estados:
- ...
Contabilidad:
- ...
Impuestos:
- ...
Inventario:
- ...
Pricing:
- ...
Notificaciones:
- ...
Archivos:
- ...
Reportes:
- ...
Aprobacion:
- Si/No
Auditoria reforzada:
- Si/No
Offline:
- Si/No
Cancelacion/Reverso:
- ...
Riesgos:
- ...
```

---

# 6. MAQUINAS DE ESTADO OBLIGATORIAS

Cada entidad critica debe tener estados y transiciones claras.

## 6.1 SalesQuotation
- DRAFT
- SENT
- APPROVAL_PENDING
- APPROVED
- ACCEPTED
- REJECTED
- EXPIRED
- CANCELLED

## 6.2 SalesOrder
- DRAFT
- CONFIRMED
- PARTIALLY_FULFILLED
- FULFILLED
- INVOICED
- CANCELLED
- RETURNED

## 6.3 PurchaseRequisition
- DRAFT
- SUBMITTED
- APPROVAL_PENDING
- APPROVED
- REJECTED
- CLOSED
- CANCELLED

## 6.4 PurchaseOrder
- DRAFT
- SENT
- APPROVED
- PARTIALLY_RECEIVED
- RECEIVED
- INVOICED
- CLOSED
- CANCELLED

## 6.5 Receivable
- OPEN
- PARTIALLY_PAID
- PROMISED
- DISPUTED
- OVERDUE
- LEGAL
- WRITTEN_OFF
- PAID
- CANCELLED

## 6.6 Payable
- OPEN
- PARTIALLY_PAID
- APPROVAL_PENDING
- DISPUTED
- DUE
- PAID
- CANCELLED

## 6.7 KitchenTicket
- NEW
- ACKNOWLEDGED
- PREPARING
- READY
- SERVED
- VOIDED

## 6.8 Reservation
- PENDING
- CONFIRMED
- SEATED
- NO_SHOW
- CANCELLED
- COMPLETED

## 6.9 Lead/Opportunity
- NEW
- QUALIFIED
- CONTACTED
- PROPOSAL
- NEGOTIATION
- WON
- LOST
- NURTURING
- DISQUALIFIED

## 6.10 Student lifecycle
- PROSPECT
- APPLICANT
- ADMITTED
- ENROLLED
- ACTIVE
- AT_RISK
- SUSPENDED
- GRADUATED
- WITHDRAWN
- ALUMNI

## 6.11 Ticket soporte
- NEW
- ASSIGNED
- IN_PROGRESS
- WAITING_CUSTOMER
- ESCALATED
- RESOLVED
- CLOSED
- REOPENED

## 6.12 Reglas
Cada maquina de estados debe definir:
- transiciones permitidas
- roles autorizados
- eventos emitidos
- side effects
- aprobaciones requeridas
- auditoria
- notificaciones
- bloqueo por periodo/cierre si aplica

---

# 7. REQUISITOS NO FUNCIONALES CONVERTIDOS EN REQUISITOS DE PRODUCTO

## 7.1 Performance
- paginacion en todos los listados
- filtros persistentes
- virtual scroll en tablas grandes
- no N+1
- indices en campos de busqueda
- consultas pesadas optimizadas
- cache en dashboards y catalogos
- debounce de 300 ms
- lazy loading por ruta

## 7.2 Confiabilidad
- idempotencia en ventas, pagos, sync, SRI y webhooks
- retry con backoff
- dead letter queue
- jobs auditables
- health checks
- readiness checks
- manejo consistente de timeouts
- colas visibles desde panel operativo

## 7.3 Seguridad
- permisos por accion
- permisos por sucursal
- permisos por bodega
- permisos por caja
- permisos por tenant
- 2FA opcional/obligatorio por rol
- bloqueo por intentos
- sesiones visibles y revocables
- segregacion de funciones
- mascaramiento de datos sensibles
- aprobacion para permisos criticos

## 7.4 Cumplimiento
- archivo documental
- retencion
- auditoria de lecturas sensibles
- consentimiento cuando aplique
- logs de descarga
- bitacora de cambios maestros

## 7.5 Observabilidad
- logs estructurados
- correlation id
- trazas
- metricas tecnicas
- metricas de negocio
- dashboard de integraciones
- dashboard de SRI
- dashboard de sync offline
- dashboard de colas y jobs

## 7.6 UX operativa
- loading state
- empty state
- error state
- confirm dialogs
- toasts
- formularios con validacion inmediata
- teclado y touch donde aplique
- accesibilidad basica
- responsive real

---

# 8. MODULOS Y CAPACIDADES FINALES

# 8.1 Core de identidad y gobierno
Debe incluir:
- login
- refresh token
- sesiones
- cierre remoto
- roles
- permisos granulares
- permisos por scope operativo
- auditoria
- bitacora de login
- impersonation auditada
- expiracion de permisos temporales
- deteccion de combinaciones peligrosas de permisos

# 8.2 Master Data Management
Debe incluir:
- deduplicacion
- merge de registros
- calidad de datos
- importacion
- exportacion
- reglas de validacion
- estado activo/inactivo/bloqueado
- historial de cambios
- enrichment basico
- validacion de cédula/RUC Ecuador

# 8.3 Contabilidad y plan de cuentas
Debe incluir:
- plan de cuentas jerarquico
- naturaleza
- cuentas de mayor y auxiliares
- diarios
- periodos
- asientos
- plantillas de asiento
- cierre preliminar
- cierre final
- consolidacion
- dimensiones analiticas
- drill-down reporte -> asiento -> documento

# 8.4 Cuentas por cobrar + cobranza + customer follow-up
Debe incluir:
- receivables
- pagos parciales
- application
- aging
- promesas de pago
- disputas
- dunning
- intereses
- gestores
- score de riesgo
- timeline de cobranza
- customer 360 completo

# 8.5 Cuentas por pagar + tesoreria
Debe incluir:
- payables
- programacion de pagos
- lotes
- bancos
- conciliacion
- transferencias
- caja chica
- rendiciones
- cash forecast
- matching y anti-duplicado

# 8.6 Presupuestos + control gerencial
Debe incluir:
- versiones de presupuesto
- por cuenta
- por centro de costo
- por sucursal
- por unidad de negocio
- forecast
- comparativos
- escenarios
- alertas de desviacion

# 8.7 Activos fijos + mantenimiento
Debe incluir:
- alta
- depreciacion
- revalorizacion
- baja
- ubicacion
- custodio
- hojas de vida
- mantenimiento preventivo
- correctivo
- ordenes de trabajo
- repuestos
- costo total de propiedad

# 8.8 Compras / P2P
Debe incluir:
- requisiciones
- RFQ
- comparativos
- OCs
- recepcion
- vendor bill
- 2-way y 3-way match
- score proveedor
- contratos marco
- lista aprobada por producto

# 8.9 Inventario / WMS
Debe incluir:
- multi-bodega
- ubicaciones
- lotes
- series
- stock reservado/disponible
- transferencias
- conteos
- quarantines
- valuacion
- reabastecimiento
- ABC/XYZ
- FEFO/FIFO donde aplique

# 8.10 CRM + ventas + customer 360
Debe incluir:
- leads
- oportunidades
- pipeline
- actividades
- scoring
- assignment
- forecast
- account plan
- timeline 360
- integracion con CxC, tickets, ventas y loyalty

# 8.11 Pricing + promociones + loyalty
Debe incluir:
- listas de precio
- reglas por cliente, canal, horario, sucursal y vigencia
- margen minimo
- aprobacion de descuentos
- combos
- 2x1
- coupons
- gift cards
- loyalty
- cashback
- membresias

# 8.12 POS avanzado
Debe incluir:
- cajas
- aperturas/cierres
- arqueos
- pagos mixtos
- impresoras
- lector
- offline
- reintentos
- devoluciones
- anulaciones
- loyalty
- gift cards
- dashboard de caja

# 8.13 Restaurante full
Debe incluir:
- floor plan
- mesas
- reservas
- waitlist
- KDS
- estaciones
- recetas
- modifiers
- combos
- self-order
- delivery/takeout
- split bill
- tiempos operativos
- costo teorico vs real
- merma

# 8.14 Educacion full
Debe incluir:
- admisiones
- academico
- cursos
- malla
- matriculas
- notas
- asistencia
- becas
- tesoreria estudiantil
- planes de pago
- portal estudiante/representante
- riesgo academico
- alertas por mora y bajo rendimiento

# 8.15 Retail omnicanal
Debe incluir:
- canales
- order routing
- click and collect
- devoluciones cross-channel
- inventory sync
- pick-pack-ship
- stock por canal
- order mapping
- dashboards por canal

# 8.16 RRHH basico + nomina
Debe incluir:
- empleados
- cargos
- contratos
- asistencia
- vacaciones
- permisos
- novedades
- rol de pagos parametrizable
- portal empleado

# 8.17 Service desk + postventa
Debe incluir:
- tickets
- SLA
- colas
- asignacion
- base de conocimiento
- portal
- encuestas
- mantenimiento
- work orders

# 8.18 Proyectos y servicios
Debe incluir:
- proyectos
- tareas
- hitos
- tiempos
- costos
- rentabilidad
- facturacion por proyecto

# 8.19 Plataforma de implantacion
Debe incluir:
- onboarding wizard
- seeds por industria
- data migration
- preview
- validacion
- rollback parcial
- tours
- checklist de go-live
- workflows configurables

# 8.20 BI y semantic layer
Debe incluir:
- dashboards ejecutivos
- dashboards operativos
- KPIs certificados
- datasets reutilizables
- alertas
- programacion de envios
- explicacion de variaciones

---

# 9. MAPA CONTABLE OBLIGATORIO POR TRANSACCION

Todo modulo monetario debe declarar su mapping.

## 9.1 Plantilla
```md
### Mapa contable - [Transaccion]
Evento:
- ...

Debe:
- cuenta ...
Haber:
- cuenta ...

Condiciones:
- ...
Centro de costo:
- ...
Dimension analitica:
- ...
Soporte documental:
- ...
Reversion:
- ...
```

## 9.2 Casos minimos que deben declararse
- venta POS
- venta factura
- nota de credito
- compra recepcionada
- vendor bill
- pago cliente
- pago proveedor
- interes por mora
- depreciacion
- beca/aplicacion descuento estudiantil
- gift card emitida/redimida
- devolucion retail
- consumo por receta si se contabiliza

---

# 10. MATRIZ DE PERMISOS MAESTRA

Cada modulo debe definir 4 niveles:

## 10.1 Scope
- tenant
- sucursal
- bodega
- caja
- programa academico
- restaurante/estacion

## 10.2 Acciones
- view
- create
- edit
- approve
- cancel
- delete
- export
- import
- override
- close
- reopen

## 10.3 Sensibilidad
- normal
- sensible
- critica

## 10.4 Politicas
- requiere MFA
- requiere doble aprobacion
- requiere comentario obligatorio
- requiere archivo adjunto
- requiere motivo de negocio

---

# 11. PAQUETIZACION COMERCIAL FINAL

## 11.1 Business Core
- contabilidad
- compras
- ventas
- inventario
- CxC/CxP
- SRI
- dashboards base

## 11.2 Business Growth
- CRM
- cobranza avanzada
- presupuestos
- activos
- pricing
- loyalty

## 11.3 Retail Pro
- POS
- pricing engine
- multi-bodega
- omnicanal
- devoluciones
- gift cards

## 11.4 Restaurant Pro
- POS restaurante
- mesas
- reservas
- KDS
- recetas
- self-order
- delivery/takeout

## 11.5 Education Suite
- admisiones
- academico
- asistencia
- colegiatura
- becas
- portales

## 11.6 Enterprise Plus
- multiempresa
- consolidacion
- workflows
- aprobaciones
- service desk
- proyectos
- RRHH

---

# 12. ROADMAP FINAL DE EJECUCION

## Fase 0
Core plataforma:
- seguridad
- permisos
- auditoria
- approvals
- workflows
- archivos
- notificaciones
- observabilidad
- MDM base

## Fase 1
Finanzas:
- plan de cuentas
- CxC/CxP
- cobranza
- presupuestos
- activos
- tesoreria
- conciliacion

## Fase 2
Inventario/WMS:
- bodegas
- lotes
- series
- conteos
- costeo
- reabastecimiento
- transferencias

## Fase 3
Compras/P2P:
- proveedores
- requisiciones
- RFQ
- OCs
- recepcion
- 3-way match

## Fase 4
CRM + ventas + customer 360

## Fase 5
Pricing + loyalty + POS + offline base

## Fase 6
Restaurante full

## Fase 7
Tax engine + SRI produccion + archivo legal

## Fase 8
Educacion full

## Fase 9
Retail omnicanal

## Fase 10
RRHH + nomina base

## Fase 11
Service desk + mantenimiento

## Fase 12
Proyectos + servicios

## Fase 13
Onboarding + migracion + kits por industria

## Fase 14
BI + semantic layer + alertas + explicaciones

---

# 13. STANDARD DE ENTREGA PARA AGENTES DE CODIGO

## 13.1 Toda entrega debe traer
- analisis funcional
- entidades reutilizadas del canon
- entidades nuevas si son inevitables
- matriz de impacto
- maquina de estados
- migracion SQL
- entidades y repositorios
- servicios
- endpoints
- DTOs
- permisos
- auditoria
- frontend Angular
- rutas
- session-context
- erp-api
- loading/empty/error
- animaciones
- tests
- checklist E2E

## 13.2 Prohibido
- duplicar tablas
- crear bypasses de permisos
- omitir impacto contable
- meter reglas fijas si deben parametrizarse
- romper design system
- cambiar contratos sin plan de compatibilidad
- ignorar tenant/sucursal
- crear UI sin estados vacios o errores

---

# 14. DEFINITION OF DONE FINAL

Un modulo solo queda terminado si cumple todos estos bloques:

## Producto
- resuelve el proceso completo
- contempla excepciones
- tiene estados
- tiene anulacion/cancelacion
- tiene comentarios/observaciones
- tiene adjuntos si aplica
- tiene KPIs minimos

## Backend
- migracion
- entidades
- repositorios
- servicios
- endpoints
- validaciones
- permisos
- auditoria
- logs
- tests
- indices

## Frontend
- ruta lazy loaded
- guard
- listado
- detalle
- formulario
- filtros
- paginacion
- export si aplica
- loading
- empty
- error
- responsive
- animaciones

## Integracion
- tenant
- sucursal
- notificaciones
- archivos
- aprobaciones
- workflow
- contabilidad si aplica
- impuestos si aplica
- BI si aplica

## Operacion
- performance aceptable
- trazabilidad
- sin romper modulos existentes
- manual de verificacion E2E

---

# 15. CHECKLIST DE PR FINAL

Antes de aprobar cualquier PR o entrega de modelo:

- usa entidades canonicas?
- evita duplicacion?
- define estados?
- define matriz de impacto?
- define permiso por accion?
- considera tenant/sucursal?
- incluye auditoria?
- incluye comentarios/adjuntos si el dominio lo necesita?
- incluye impacto contable?
- incluye impacto tributario?
- incluye loading/empty/error?
- incluye paginacion y filtros?
- incluye tests?
- incluye estrategia de cancelacion o reverso?
- respeta design system?
- no rompe lo existente?

---

# 16. PROMPTS FINALES PARA CODEX / CLAUDE

## 16.1 Prompt maestro absoluto
```md
Estas trabajando sobre un ERP multi-tenant, multi-industria y multi-sucursal para Ecuador y LATAM.
Debes seguir estrictamente el PLAN MAESTRO DEFINITIVO V3.

Objetivo:
construir capacidades empresariales completas, no funciones sueltas.

Reglas absolutas:
1. Reutiliza el nucleo canonico de entidades. No dupliques modelos.
2. Toda feature debe declarar matriz de impacto.
3. Toda entidad critica debe tener maquina de estados.
4. Toda operacion monetaria debe declarar mapping contable.
5. Toda accion sensible debe tener permisos y auditoria.
6. Toda pantalla debe incluir loading, empty, error y animaciones suaves.
7. Todo listado debe tener filtros, paginacion y export si aplica.
8. Debes respetar tenant, sucursal, bodega, caja y permisos existentes.
9. No rompas SRI, POS, multi-tenant ni el design system.
10. Debes entregar backend + frontend + DTOs + validaciones + pruebas + checklist E2E.
11. Si detectas un hueco funcional critico, debes corregirlo en la propuesta.
12. No hardcodees reglas que deban ser parametrizables.
```

## 16.2 Prompt por modulo
```md
Construye el modulo [NOMBRE] siguiendo el PLAN MAESTRO DEFINITIVO V3.

Entrega obligatoria:
- objetivo funcional
- entidades canonicas reutilizadas
- entidades nuevas si son inevitables
- matriz de impacto
- maquina de estados
- migracion SQL
- entidades/repositorios
- servicios
- endpoints
- DTOs
- permisos
- auditoria
- integracion con notificaciones, archivos, aprobaciones y workflows
- impacto contable e impacto tributario si aplica
- componentes Angular
- rutas
- session-context
- erp-api
- loading/empty/error
- animaciones
- tests
- checklist E2E

No omitas:
- cancelacion/anulacion
- observaciones
- adjuntos
- exportacion
- filtros
- paginacion
- errores claros
```

## 16.3 Prompt de refactor
```md
Refactoriza el modulo [NOMBRE] para alinearlo al PLAN MAESTRO DEFINITIVO V3.

Debes detectar:
- entidades duplicadas
- huecos funcionales
- estados faltantes
- validaciones faltantes
- errores de permisos
- falta de auditoria
- falta de integracion contable
- falta de integracion tributaria
- problemas de UX
- problemas de performance
- endpoints inconsistentes
- queries costosas
- tablas sin indices

Entrega un plan por etapas sin romper compatibilidad, con cambios de backend, frontend, DTOs, DB y verificaciones.
```

---

# 17. REGLA FINAL DE GOBIERNO

## Si una mejora no cumple estas 5 cosas, no entra:
- conecta con el modelo canonico
- resuelve un proceso completo
- deja trazabilidad y permisos claros
- puede venderse como capacidad real
- no rompe la consistencia financiera/tributaria/operativa

---

# 18. CONCLUSION FINAL

Esta V3 deja el ERP en un nivel donde la mejora adicional ya no depende de “inventar mas modulos”, sino de ejecutar con disciplina.

Con esta version:
- ya hay vision de producto
- ya hay nucleo canonico
- ya hay matriz de impacto
- ya hay estados
- ya hay mapa contable
- ya hay matriz de permisos
- ya hay roadmap
- ya hay paquetes comerciales
- ya hay Definition of Done
- ya hay prompts finales para agentes

Regla terminal del proyecto:

**No agregar tablas, pantallas o endpoints aislados. Construir dominios empresariales completos, consistentes, auditables, parametrizables, comercializables y listos para Ecuador.**
