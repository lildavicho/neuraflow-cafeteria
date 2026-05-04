import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ErpApi,
  PartyDto,
  PartyDetailDto,
  PartyIdentifierDto,
  PartyAddressDto,
  PartyIdentifierPayload,
  PartyAddressPayload,
} from '../../core/services/erp-api';
import { ToastService } from '../../core/services/toast';
import { Paginator } from '../shared/components/paginator';

type RoleTab = 'ALL' | 'CUSTOMER' | 'SUPPLIER' | 'EMPLOYEE' | 'STUDENT';

interface IdentifierFormState {
  id: number | null;
  type: string;
  value: string;
  isPrimary: boolean;
}

interface AddressFormState {
  id: number | null;
  type: string;
  street: string;
  city: string;
  province: string;
  postalCode: string;
  country: string;
  isPrimary: boolean;
}

@Component({
  selector: 'app-parties',
  imports: [FormsModule, Paginator],
  template: `
    <section class="page">

      <!-- ══ HEADER ═════════════════════════════════════════════════════ -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Datos maestros</span>
          <h1>Contactos</h1>
          <p>Directorio unificado de clientes, proveedores, empleados y estudiantes.</p>
        </div>
        <div class="page__acts">
          <button class="btn-primary" (click)="openCreate()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:18px;height:18px">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
            </svg>
            Nuevo contacto
          </button>
        </div>
      </header>

      <!-- ══ KPI STRIP ══════════════════════════════════════════════════ -->
      <div class="kpi-row">
        @for (kpi of kpis; track kpi.role) {
          <div class="kpi" [class.kpi--active]="roleTab() === kpi.role" (click)="roleTab.set(kpi.role); reload()">
            <span class="kpi__label">{{ kpi.label }}</span>
            <span class="kpi__val">{{ counts()[kpi.countKey] || 0 }}</span>
          </div>
        }
      </div>

      <!-- ══ TABLE ══════════════════════════════════════════════════════ -->
      <section class="card">
        <header class="card__hd">
          <div><span class="eyebrow">Directorio</span><h2>Contactos registrados</h2></div>
          <div class="tbl-controls">
            <input class="search-field" [ngModel]="search()" (ngModelChange)="search.set($event)"
                   (keyup.enter)="reload()" placeholder="Buscar nombre, email, identificacion..."/>
            <button class="btn-outline btn--sm" (click)="reload()">Buscar</button>
          </div>
        </header>

        @if (loading()) {
          <p class="state-msg">Cargando contactos...</p>
        } @else if (errorMsg()) {
          <p class="state-msg state-msg--error">
            {{ errorMsg() }}
            <button class="link-btn" (click)="reload()">Reintentar</button>
          </p>
        } @else if (!parties().length) {
          <p class="state-msg">
            No hay contactos registrados.
          </p>
        } @else {
          <div class="tbl">
            <div class="tbl__head">
              <span>Nombre</span>
              <span>Tipo</span>
              <span>Identificacion</span>
              <span>Email</span>
              <span>Telefono</span>
              <span>Roles</span>
              <span>Estado</span>
              <span>Acciones</span>
            </div>

            @for (p of parties(); track p.id) {
              <div class="tbl__row" (click)="openDetail(p.id)">
                <span class="cell-name">{{ p.displayName }}</span>
                <span class="muted">{{ p.partyType === 'PERSON' ? 'Persona' : 'Organizacion' }}</span>
                <span class="mono">{{ p.primaryIdentifierValue || '—' }}</span>
                <span class="muted">{{ p.email || '—' }}</span>
                <span class="muted">{{ p.phone || '—' }}</span>
                <span>
                  @for (r of p.roles; track r) {
                    <span class="role-chip">{{ roleLabel(r) }}</span>
                  }
                </span>
                <span>
                  <span class="badge" [class.badge--ok]="p.active" [class.badge--warn]="!p.active">
                    {{ p.active ? 'Activo' : 'Inactivo' }}
                  </span>
                </span>
                <div class="tbl__acts" (click)="$event.stopPropagation()">
                  <button class="act-btn" (click)="openDetail(p.id)">Ver</button>
                  <button class="act-btn" (click)="openEditFromList(p)">Editar</button>
                </div>
              </div>
            }
          </div>

          <app-paginator
            [page]="page()"
            [pageSize]="pageSize"
            [totalElements]="totalElements()"
            (pageChange)="page.set($event); reload()"
          />
        }
      </section>

      <!-- ══ MODAL: CREATE/EDIT ═════════════════════════════════════════ -->
      @if (showModal()) {
        <div class="modal-bd" (click)="closeModal()">
          <div class="modal-box modal-box--lg" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <h3>{{ editing() ? 'Editar contacto' : 'Nuevo contacto' }}</h3>
              <button class="modal__close" (click)="closeModal()">&times;</button>
            </header>
            <form class="modal__body" (ngSubmit)="save()">
              <div class="form-grid">
                <label class="field">
                  <span class="field__label">Tipo</span>
                  <select class="field__input" [(ngModel)]="form.partyType" name="partyType">
                    <option value="PERSON">Persona</option>
                    <option value="ORGANIZATION">Organizacion</option>
                  </select>
                </label>
                <label class="field">
                  <span class="field__label">Nombre visible *</span>
                  <input class="field__input" [(ngModel)]="form.displayName" name="displayName" required maxlength="200"/>
                </label>
                @if (form.partyType === 'PERSON') {
                  <label class="field">
                    <span class="field__label">Nombres</span>
                    <input class="field__input" [(ngModel)]="form.firstName" name="firstName" maxlength="100"/>
                  </label>
                  <label class="field">
                    <span class="field__label">Apellidos</span>
                    <input class="field__input" [(ngModel)]="form.lastName" name="lastName" maxlength="100"/>
                  </label>
                }
                @if (form.partyType === 'ORGANIZATION') {
                  <label class="field" style="grid-column:1/-1">
                    <span class="field__label">Razon social</span>
                    <input class="field__input" [(ngModel)]="form.legalName" name="legalName" maxlength="200"/>
                  </label>
                }
                <label class="field">
                  <span class="field__label">Tipo identificacion</span>
                  <select class="field__input" [(ngModel)]="form.identifierType" name="identifierType">
                    <option value="">Sin identificacion</option>
                    <option value="CEDULA">Cedula</option>
                    <option value="RUC">RUC</option>
                    <option value="PASSPORT">Pasaporte</option>
                    <option value="FOREIGN_ID">ID Extranjero</option>
                  </select>
                </label>
                <label class="field">
                  <span class="field__label">Numero identificacion</span>
                  <input class="field__input" [(ngModel)]="form.identifierValue" name="identifierValue" maxlength="50"
                         placeholder="Ej: 0102030405"/>
                </label>
                <label class="field">
                  <span class="field__label">Email</span>
                  <input class="field__input" type="email" [(ngModel)]="form.email" name="email" maxlength="150"/>
                </label>
                <label class="field">
                  <span class="field__label">Telefono</span>
                  <input class="field__input" [(ngModel)]="form.phone" name="phone" maxlength="30"/>
                </label>
                <label class="field">
                  <span class="field__label">Celular</span>
                  <input class="field__input" [(ngModel)]="form.mobile" name="mobile" maxlength="30"/>
                </label>
                <label class="field">
                  <span class="field__label">Direccion</span>
                  <input class="field__input" [(ngModel)]="form.street" name="street" maxlength="300"/>
                </label>
                <label class="field">
                  <span class="field__label">Ciudad</span>
                  <input class="field__input" [(ngModel)]="form.city" name="city" maxlength="100"/>
                </label>
                <label class="field">
                  <span class="field__label">Provincia</span>
                  <input class="field__input" [(ngModel)]="form.province" name="province" maxlength="100"/>
                </label>

                @if (!editing()) {
                  <fieldset class="field" style="grid-column:1/-1; border:1px solid var(--c-border); border-radius:8px; padding:0.75rem;">
                    <legend class="field__label" style="padding:0 0.5rem">Roles</legend>
                    <div style="display:flex; gap:1rem; flex-wrap:wrap;">
                      @for (opt of roleOptions; track opt.value) {
                        <label style="display:flex; align-items:center; gap:0.375rem; font-size:0.8125rem; cursor:pointer;">
                          <input type="checkbox" [checked]="form.roles.includes(opt.value)"
                                 (change)="toggleRole(opt.value)" style="accent-color:var(--c-aurora)"/>
                          {{ opt.label }}
                        </label>
                      }
                    </div>
                  </fieldset>
                }

                <label class="field" style="grid-column:1/-1">
                  <span class="field__label">Notas</span>
                  <textarea class="field__input" [(ngModel)]="form.notes" name="notes" rows="2" style="resize:vertical"></textarea>
                </label>
              </div>
              <footer class="modal__ft">
                <button type="button" class="btn-outline" (click)="closeModal()">Cancelar</button>
                <button type="submit" class="btn-primary" [disabled]="saving()">
                  {{ saving() ? 'Guardando...' : (editing() ? 'Guardar cambios' : 'Crear contacto') }}
                </button>
              </footer>
            </form>
          </div>
        </div>
      }

      <!-- ══ DETAIL PANEL ═══════════════════════════════════════════════ -->
      @if (detail()) {
        <div class="modal-bd" (click)="closeDetail()">
          <div class="modal-box modal-box--lg" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <div>
                <h3>{{ detail()!.displayName }}</h3>
                <span class="muted" style="font-size:0.75rem">
                  {{ detail()!.partyType === 'PERSON' ? 'Persona' : 'Organizacion' }}
                  @if (!detail()!.active) { · <span style="color:var(--c-warn)">Inactivo</span> }
                </span>
              </div>
              <button class="modal__close" (click)="closeDetail()">&times;</button>
            </header>

            <nav class="tabs">
              <button class="tab" [class.tab--active]="detailTab() === 'general'"
                      (click)="detailTab.set('general')">General</button>
              <button class="tab" [class.tab--active]="detailTab() === 'identifiers'"
                      (click)="detailTab.set('identifiers')">
                Identificadores ({{ detail()!.identifiers.length }})
              </button>
              <button class="tab" [class.tab--active]="detailTab() === 'addresses'"
                      (click)="detailTab.set('addresses')">
                Direcciones ({{ detail()!.addresses.length }})
              </button>
              <button class="tab" [class.tab--active]="detailTab() === 'roles'"
                      (click)="detailTab.set('roles')">
                Roles ({{ detail()!.roles.length }})
              </button>
            </nav>

            <div class="modal__body">
              @if (detailTab() === 'general') {
                <div class="detail-grid">
                  @if (detail()!.legalName) {
                    <div class="detail-item"><span class="detail-label">Razon social</span><span>{{ detail()!.legalName }}</span></div>
                  }
                  @if (detail()!.firstName) {
                    <div class="detail-item"><span class="detail-label">Nombres</span><span>{{ detail()!.firstName }} {{ detail()!.lastName }}</span></div>
                  }
                  <div class="detail-item"><span class="detail-label">Email</span><span>{{ detail()!.email || '—' }}</span></div>
                  <div class="detail-item"><span class="detail-label">Telefono</span><span>{{ detail()!.phone || '—' }}</span></div>
                  <div class="detail-item"><span class="detail-label">Celular</span><span>{{ detail()!.mobile || '—' }}</span></div>
                  <div class="detail-item"><span class="detail-label">Estado</span>
                    <span class="badge" [class.badge--ok]="detail()!.active" [class.badge--warn]="!detail()!.active">
                      {{ detail()!.active ? 'Activo' : 'Inactivo' }}
                    </span>
                  </div>
                  @if (detail()!.notes) {
                    <div class="detail-item" style="grid-column:1/-1">
                      <span class="detail-label">Notas</span>
                      <p style="font-size:0.8125rem;white-space:pre-wrap;margin:0">{{ detail()!.notes }}</p>
                    </div>
                  }
                </div>
              }

              @if (detailTab() === 'identifiers') {
                <div class="sub-toolbar">
                  <span class="muted" style="font-size:0.8125rem">Cedulas, RUC, pasaportes y otros documentos.</span>
                  <button class="btn-outline btn--sm" (click)="openIdentifierForm()">+ Agregar</button>
                </div>
                @if (!detail()!.identifiers.length) {
                  <p class="state-msg">Sin identificadores registrados.</p>
                } @else {
                  <ul class="sub-list">
                    @for (id of detail()!.identifiers; track id.id) {
                      <li class="sub-row">
                        <div>
                          <span class="role-chip">{{ id.type }}</span>
                          <span class="mono" style="margin-left:0.5rem">{{ id.value }}</span>
                          @if (id.isPrimary) {
                            <span class="badge badge--ok" style="margin-left:0.5rem">Principal</span>
                          }
                        </div>
                        <div class="sub-row__acts">
                          @if (!id.isPrimary) {
                            <button class="act-btn" (click)="markIdentifierPrimary(id)">Marcar principal</button>
                          }
                          <button class="act-btn" (click)="openIdentifierForm(id)">Editar</button>
                          <button class="act-btn act-btn--danger" (click)="removeIdentifier(id)">Eliminar</button>
                        </div>
                      </li>
                    }
                  </ul>
                }
              }

              @if (detailTab() === 'addresses') {
                <div class="sub-toolbar">
                  <span class="muted" style="font-size:0.8125rem">Direcciones de envío, facturación y otras ubicaciones.</span>
                  <button class="btn-outline btn--sm" (click)="openAddressForm()">+ Agregar</button>
                </div>
                @if (!detail()!.addresses.length) {
                  <p class="state-msg">Sin direcciones registradas.</p>
                } @else {
                  <ul class="sub-list">
                    @for (a of detail()!.addresses; track a.id) {
                      <li class="sub-row">
                        <div>
                          <span class="role-chip">{{ a.type }}</span>
                          <p style="font-size:0.8125rem;margin:0.25rem 0">
                            {{ a.street }}<span class="muted">, {{ a.city || '—' }}, {{ a.province || '—' }} ({{ a.country }})</span>
                          </p>
                          @if (a.isPrimary) {
                            <span class="badge badge--ok">Principal</span>
                          }
                        </div>
                        <div class="sub-row__acts">
                          @if (!a.isPrimary) {
                            <button class="act-btn" (click)="markAddressPrimary(a)">Marcar principal</button>
                          }
                          <button class="act-btn" (click)="openAddressForm(a)">Editar</button>
                          <button class="act-btn act-btn--danger" (click)="removeAddress(a)">Eliminar</button>
                        </div>
                      </li>
                    }
                  </ul>
                }
              }

              @if (detailTab() === 'roles') {
                <div style="display:flex;gap:0.375rem;flex-wrap:wrap">
                  @for (r of detail()!.roles; track r) {
                    <span class="role-chip role-chip--lg">{{ roleLabel(r) }}</span>
                  }
                  @if (!detail()!.roles.length) {
                    <span class="muted">Sin roles asignados</span>
                  }
                </div>
              }
            </div>

            <footer class="modal__ft">
              <button class="btn-outline" (click)="closeDetail()">Cerrar</button>
              <button class="btn-primary" (click)="openEditFromDetail()">Editar contacto</button>
            </footer>
          </div>
        </div>
      }

      <!-- ══ MODAL: IDENTIFIER FORM ═════════════════════════════════════ -->
      @if (identifierForm()) {
        <div class="modal-bd" (click)="closeIdentifierForm()">
          <div class="modal-box" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <h3>{{ identifierForm()!.id ? 'Editar identificador' : 'Nuevo identificador' }}</h3>
              <button class="modal__close" (click)="closeIdentifierForm()">&times;</button>
            </header>
            <form class="modal__body" (ngSubmit)="saveIdentifier()">
              <div class="form-grid">
                <label class="field">
                  <span class="field__label">Tipo *</span>
                  <select class="field__input" [(ngModel)]="identifierForm()!.type" name="idType" required>
                    <option value="CEDULA">Cedula</option>
                    <option value="RUC">RUC</option>
                    <option value="PASSPORT">Pasaporte</option>
                    <option value="FOREIGN_ID">ID Extranjero</option>
                  </select>
                </label>
                <label class="field">
                  <span class="field__label">Valor *</span>
                  <input class="field__input" [(ngModel)]="identifierForm()!.value" name="idValue" required maxlength="50"/>
                </label>
                <label class="field" style="grid-column:1/-1; display:flex; flex-direction:row; align-items:center; gap:0.5rem;">
                  <input type="checkbox" [(ngModel)]="identifierForm()!.isPrimary" name="idPrimary"
                         style="accent-color:var(--c-aurora)"/>
                  <span class="field__label" style="margin:0">Marcar como principal</span>
                </label>
              </div>
              <footer class="modal__ft" style="padding:1rem 0 0">
                <button type="button" class="btn-outline" (click)="closeIdentifierForm()">Cancelar</button>
                <button type="submit" class="btn-primary" [disabled]="savingChild()">
                  {{ savingChild() ? 'Guardando...' : 'Guardar' }}
                </button>
              </footer>
            </form>
          </div>
        </div>
      }

      <!-- ══ MODAL: ADDRESS FORM ════════════════════════════════════════ -->
      @if (addressForm()) {
        <div class="modal-bd" (click)="closeAddressForm()">
          <div class="modal-box" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <h3>{{ addressForm()!.id ? 'Editar dirección' : 'Nueva dirección' }}</h3>
              <button class="modal__close" (click)="closeAddressForm()">&times;</button>
            </header>
            <form class="modal__body" (ngSubmit)="saveAddress()">
              <div class="form-grid">
                <label class="field">
                  <span class="field__label">Tipo</span>
                  <select class="field__input" [(ngModel)]="addressForm()!.type" name="addrType">
                    <option value="MAIN">Principal</option>
                    <option value="BILLING">Facturacion</option>
                    <option value="SHIPPING">Envio</option>
                    <option value="OTHER">Otra</option>
                  </select>
                </label>
                <label class="field">
                  <span class="field__label">Pais</span>
                  <input class="field__input" [(ngModel)]="addressForm()!.country" name="addrCountry" maxlength="5"/>
                </label>
                <label class="field" style="grid-column:1/-1">
                  <span class="field__label">Calle *</span>
                  <input class="field__input" [(ngModel)]="addressForm()!.street" name="addrStreet" required maxlength="300"/>
                </label>
                <label class="field">
                  <span class="field__label">Ciudad</span>
                  <input class="field__input" [(ngModel)]="addressForm()!.city" name="addrCity" maxlength="100"/>
                </label>
                <label class="field">
                  <span class="field__label">Provincia</span>
                  <input class="field__input" [(ngModel)]="addressForm()!.province" name="addrProvince" maxlength="100"/>
                </label>
                <label class="field">
                  <span class="field__label">Codigo postal</span>
                  <input class="field__input" [(ngModel)]="addressForm()!.postalCode" name="addrPostal" maxlength="20"/>
                </label>
                <label class="field" style="display:flex; flex-direction:row; align-items:center; gap:0.5rem; align-self:end;">
                  <input type="checkbox" [(ngModel)]="addressForm()!.isPrimary" name="addrPrimary"
                         style="accent-color:var(--c-aurora)"/>
                  <span class="field__label" style="margin:0">Principal</span>
                </label>
              </div>
              <footer class="modal__ft" style="padding:1rem 0 0">
                <button type="button" class="btn-outline" (click)="closeAddressForm()">Cancelar</button>
                <button type="submit" class="btn-primary" [disabled]="savingChild()">
                  {{ savingChild() ? 'Guardando...' : 'Guardar' }}
                </button>
              </footer>
            </form>
          </div>
        </div>
      }

    </section>
  `,
  styles: `
    .page { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }
    .page__header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
    .page__header h1 { font-family: var(--ff-display); font-size: 1.75rem; color: var(--c-text); margin: 0.25rem 0 0.375rem; }
    .page__header p { color: var(--c-text-muted); font-size: 0.875rem; max-width: 36rem; }
    .page__acts { display: flex; gap: 0.5rem; align-items: center; }
    .eyebrow { font-size: 0.6875rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--c-aurora); font-weight: 600; }

    .kpi-row { display: flex; gap: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
    .kpi { flex: 1 1 140px; background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 12px; padding: 1rem 1.25rem; cursor: pointer; transition: border-color var(--dur-fast) var(--ease-out); }
    .kpi:hover { border-color: var(--c-aurora); }
    .kpi--active { border-color: var(--c-aurora); background: color-mix(in srgb, var(--c-aurora) 5%, var(--c-panel)); }
    .kpi__label { font-size: 0.75rem; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.06em; display: block; margin-bottom: 0.25rem; }
    .kpi__val { font-family: var(--ff-display); font-size: 1.5rem; color: var(--c-text); }

    .card { background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 12px; overflow: hidden; }
    .card__hd { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid var(--c-border); flex-wrap: wrap; gap: 0.75rem; }
    .card__hd h2 { font-family: var(--ff-display); font-size: 1.125rem; margin: 0.125rem 0 0; color: var(--c-text); }

    .tbl-controls { display: flex; gap: 0.5rem; align-items: center; }
    .search-field { padding: 0.5rem 0.75rem; border: 1px solid var(--c-border); border-radius: 8px; font-size: 0.8125rem; background: var(--c-bg); color: var(--c-text); width: 260px; }
    .search-field:focus { outline: none; border-color: var(--c-aurora); box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-aurora) 15%, transparent); }

    .tbl { display: grid; }
    .tbl__head { display: grid; grid-template-columns: 1fr 100px 130px 160px 110px 140px 80px 120px; padding: 0.625rem 1.25rem; font-size: 0.6875rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--c-text-muted); border-bottom: 1px solid var(--c-border); }
    .tbl__row { display: grid; grid-template-columns: 1fr 100px 130px 160px 110px 140px 80px 120px; padding: 0.75rem 1.25rem; align-items: center; font-size: 0.8125rem; border-bottom: 1px solid var(--c-border-soft, var(--c-border)); cursor: pointer; transition: background var(--dur-fast) var(--ease-out); }
    .tbl__row:hover { background: color-mix(in srgb, var(--c-aurora) 4%, transparent); }
    .tbl__acts { display: flex; gap: 0.375rem; }

    .mono { font-family: 'DM Sans', monospace; font-size: 0.8125rem; letter-spacing: 0.02em; }
    .muted { color: var(--c-text-muted); }
    .cell-name { font-weight: 500; color: var(--c-text); }

    .badge { display: inline-block; padding: 0.125rem 0.5rem; border-radius: 6px; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; }
    .badge--ok { background: color-mix(in srgb, var(--c-ok) 12%, transparent); color: var(--c-ok); }
    .badge--warn { background: color-mix(in srgb, var(--c-warn) 12%, transparent); color: var(--c-warn); }

    .role-chip { display: inline-block; padding: 0.125rem 0.5rem; border-radius: 6px; font-size: 0.625rem; font-weight: 600; background: color-mix(in srgb, var(--c-aurora) 10%, transparent); color: var(--c-aurora); text-transform: uppercase; letter-spacing: 0.04em; margin-right: 0.25rem; }
    .role-chip--lg { font-size: 0.75rem; padding: 0.25rem 0.625rem; }

    .btn-primary { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.5rem 1rem; background: var(--c-aurora); color: #fff; border: none; border-radius: 8px; font-size: 0.8125rem; font-weight: 600; cursor: pointer; transition: opacity var(--dur-fast) var(--ease-out); }
    .btn-primary:hover { opacity: 0.9; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.5rem 1rem; background: transparent; color: var(--c-text); border: 1px solid var(--c-border); border-radius: 8px; font-size: 0.8125rem; font-weight: 500; cursor: pointer; }
    .btn-outline:hover { border-color: var(--c-aurora); color: var(--c-aurora); }
    .btn--sm { padding: 0.375rem 0.75rem; font-size: 0.75rem; }
    .act-btn { padding: 0.25rem 0.625rem; font-size: 0.75rem; border: 1px solid var(--c-border); border-radius: 6px; background: var(--c-panel); color: var(--c-text); cursor: pointer; }
    .act-btn:hover { border-color: var(--c-aurora); color: var(--c-aurora); }

    .state-msg { padding: 3rem 1.25rem; text-align: center; color: var(--c-text-muted); font-size: 0.875rem; }
    .state-msg--error { color: var(--c-danger); }
    .link-btn { background: none; border: none; color: var(--c-aurora); font-size: inherit; cursor: pointer; text-decoration: underline; }

    .modal-bd { position: fixed; inset: 0; z-index: 900; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.45); backdrop-filter: blur(4px); animation: fadeIn var(--dur-fast) var(--ease-out); }
    .modal-box { background: var(--c-panel); border-radius: 16px; width: min(560px, 94vw); max-height: 90vh; overflow-y: auto; box-shadow: 0 24px 48px rgba(0,0,0,0.18); animation: slideUp var(--dur-normal) var(--ease-out); }
    .modal-box--lg { width: min(680px, 96vw); }
    .modal__hd { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--c-border); }
    .modal__hd h3 { font-family: var(--ff-display); font-size: 1.125rem; margin: 0; color: var(--c-text); }
    .modal__close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--c-text-muted); line-height: 1; }
    .modal__body { padding: 1.5rem; }
    .modal__ft { display: flex; justify-content: flex-end; gap: 0.5rem; padding: 1rem 1.5rem; border-top: 1px solid var(--c-border); }

    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .field { display: flex; flex-direction: column; gap: 0.25rem; }
    .field__label { font-size: 0.75rem; font-weight: 600; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.04em; }
    .field__input { padding: 0.5rem 0.75rem; border: 1px solid var(--c-border); border-radius: 8px; font-size: 0.8125rem; background: var(--c-bg); color: var(--c-text); }
    .field__input:focus { outline: none; border-color: var(--c-aurora); box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-aurora) 15%, transparent); }

    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .detail-item { display: flex; flex-direction: column; gap: 0.25rem; }
    .detail-label { font-size: 0.6875rem; font-weight: 600; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.04em; }

    .tabs { display: flex; gap: 0; padding: 0 1.5rem; border-bottom: 1px solid var(--c-border); background: color-mix(in srgb, var(--c-aurora) 3%, transparent); }
    .tab { background: none; border: none; padding: 0.75rem 1rem; font-size: 0.8125rem; font-weight: 500; color: var(--c-text-muted); cursor: pointer; border-bottom: 2px solid transparent; transition: color var(--dur-fast) var(--ease-out), border-color var(--dur-fast) var(--ease-out); }
    .tab:hover { color: var(--c-text); }
    .tab--active { color: var(--c-aurora); border-bottom-color: var(--c-aurora); font-weight: 600; }

    .sub-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 0.75rem; }
    .sub-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem; }
    .sub-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 0.75rem 1rem; border: 1px solid var(--c-border); border-radius: 8px; background: var(--c-bg); }
    .sub-row__acts { display: flex; gap: 0.375rem; flex-wrap: wrap; }
    .act-btn--danger { color: var(--c-danger); border-color: color-mix(in srgb, var(--c-danger) 35%, var(--c-border)); }
    .act-btn--danger:hover { background: color-mix(in srgb, var(--c-danger) 8%, transparent); border-color: var(--c-danger); color: var(--c-danger); }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

    @media (max-width: 768px) {
      .tbl__head, .tbl__row { grid-template-columns: 1fr 90px 100px 80px 90px; }
      .tbl__head > :nth-child(4), .tbl__row > :nth-child(4),
      .tbl__head > :nth-child(5), .tbl__row > :nth-child(5),
      .tbl__head > :nth-child(6), .tbl__row > :nth-child(6) { display: none; }
      .form-grid, .detail-grid { grid-template-columns: 1fr; }
    }
  `,
})
export class Parties implements OnInit {
  private readonly api = inject(ErpApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ───────────────────────────────────────────────────────────
  parties = signal<PartyDto[]>([]);
  totalElements = signal(0);
  counts = signal<Record<string, number>>({});
  loading = signal(true);
  errorMsg = signal('');
  search = signal('');
  roleTab = signal<RoleTab>('ALL');
  page = signal(0);
  pageSize = 20;

  showModal = signal(false);
  editing = signal(false);
  editId = signal<number | null>(null);
  saving = signal(false);
  detail = signal<PartyDetailDto | null>(null);
  detailTab = signal<'general' | 'identifiers' | 'addresses' | 'roles'>('general');

  identifierForm = signal<IdentifierFormState | null>(null);
  addressForm = signal<AddressFormState | null>(null);
  savingChild = signal(false);

  form = {
    partyType: 'PERSON' as 'PERSON' | 'ORGANIZATION',
    displayName: '',
    legalName: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    mobile: '',
    notes: '',
    identifierType: '',
    identifierValue: '',
    street: '',
    city: '',
    province: '',
    roles: [] as string[],
  };

  kpis = [
    { role: 'ALL' as RoleTab, label: 'Todos', countKey: 'total' },
    { role: 'CUSTOMER' as RoleTab, label: 'Clientes', countKey: 'customers' },
    { role: 'SUPPLIER' as RoleTab, label: 'Proveedores', countKey: 'suppliers' },
    { role: 'EMPLOYEE' as RoleTab, label: 'Empleados', countKey: 'employees' },
    { role: 'STUDENT' as RoleTab, label: 'Estudiantes', countKey: 'students' },
  ];

  roleOptions = [
    { value: 'CUSTOMER', label: 'Cliente' },
    { value: 'SUPPLIER', label: 'Proveedor' },
    { value: 'EMPLOYEE', label: 'Empleado' },
    { value: 'STUDENT', label: 'Estudiante' },
  ];

  // ── Lifecycle ───────────────────────────────────────────────────────
  ngOnInit() {
    this.reload();
    this.loadCounts();
  }

  reload() {
    this.loading.set(true);
    this.errorMsg.set('');
    const role = this.roleTab() === 'ALL' ? undefined : this.roleTab();
    const q = this.search().trim() || undefined;
    this.api.getParties(q, role, this.page(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          this.parties.set(data.content);
          this.totalElements.set(data.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.errorMsg.set('Error al cargar contactos.');
          this.loading.set(false);
        },
      });
  }

  loadCounts() {
    this.api.getPartyCounts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          const total = (data['customers'] ?? 0) + (data['suppliers'] ?? 0) +
                        (data['employees'] ?? 0) + (data['students'] ?? 0);
          this.counts.set({ ...data, total });
        },
      });
  }

  roleLabel(role: string): string {
    switch (role) {
      case 'CUSTOMER': return 'Cliente';
      case 'SUPPLIER': return 'Proveedor';
      case 'EMPLOYEE': return 'Empleado';
      case 'STUDENT': return 'Estudiante';
      case 'GUARDIAN': return 'Representante';
      default: return role;
    }
  }

  toggleRole(role: string) {
    const idx = this.form.roles.indexOf(role);
    if (idx >= 0) this.form.roles.splice(idx, 1);
    else this.form.roles.push(role);
  }

  // ── Modal ───────────────────────────────────────────────────────────
  openCreate() {
    this.editing.set(false);
    this.editId.set(null);
    this.resetForm();
    this.showModal.set(true);
  }

  openEditFromList(p: PartyDto) {
    this.api.getParty(p.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: d => this.fillEditForm(d) });
  }

  openEditFromDetail() {
    const d = this.detail();
    if (d) {
      this.closeDetail();
      this.fillEditForm(d);
    }
  }

  openDetail(id: number) {
    this.detailTab.set('general');
    this.api.getParty(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: d => this.detail.set(d),
        error: () => this.toast.error('Error al cargar detalle.'),
      });
  }

  closeDetail() {
    this.detail.set(null);
    this.identifierForm.set(null);
    this.addressForm.set(null);
  }

  closeModal() {
    this.showModal.set(false);
  }

  // ── Identifier CRUD ────────────────────────────────────────────────
  openIdentifierForm(existing?: PartyIdentifierDto) {
    this.identifierForm.set({
      id: existing?.id ?? null,
      type: existing?.type ?? 'CEDULA',
      value: existing?.value ?? '',
      isPrimary: existing?.isPrimary ?? false,
    });
  }

  closeIdentifierForm() {
    this.identifierForm.set(null);
  }

  saveIdentifier() {
    const form = this.identifierForm();
    const party = this.detail();
    if (!form || !party) return;
    if (!form.value.trim()) {
      this.toast.warning('El valor del identificador es obligatorio.');
      return;
    }
    this.savingChild.set(true);
    const payload: PartyIdentifierPayload = {
      type: form.type,
      value: form.value.trim(),
      isPrimary: form.isPrimary,
    };
    const obs = form.id
      ? this.api.updatePartyIdentifier(party.id, form.id, payload)
      : this.api.addPartyIdentifier(party.id, payload);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success('Identificador guardado.');
        this.savingChild.set(false);
        this.closeIdentifierForm();
        this.refreshDetail();
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'No se pudo guardar.');
        this.savingChild.set(false);
      },
    });
  }

  removeIdentifier(id: PartyIdentifierDto) {
    const party = this.detail();
    if (!party) return;
    if (!confirm(`Eliminar identificador ${id.type}: ${id.value}?`)) return;
    this.api.removePartyIdentifier(party.id, id.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.toast.success('Identificador eliminado.'); this.refreshDetail(); },
        error: () => this.toast.error('No se pudo eliminar.'),
      });
  }

  markIdentifierPrimary(id: PartyIdentifierDto) {
    const party = this.detail();
    if (!party) return;
    this.api.setPrimaryPartyIdentifier(party.id, id.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.toast.success('Identificador marcado como principal.'); this.refreshDetail(); },
        error: () => this.toast.error('No se pudo actualizar.'),
      });
  }

  // ── Address CRUD ───────────────────────────────────────────────────
  openAddressForm(existing?: PartyAddressDto) {
    this.addressForm.set({
      id: existing?.id ?? null,
      type: existing?.type ?? 'MAIN',
      street: existing?.street ?? '',
      city: existing?.city ?? '',
      province: existing?.province ?? '',
      postalCode: existing?.postalCode ?? '',
      country: existing?.country ?? 'EC',
      isPrimary: existing?.isPrimary ?? false,
    });
  }

  closeAddressForm() {
    this.addressForm.set(null);
  }

  saveAddress() {
    const form = this.addressForm();
    const party = this.detail();
    if (!form || !party) return;
    if (!form.street.trim()) {
      this.toast.warning('La calle es obligatoria.');
      return;
    }
    this.savingChild.set(true);
    const payload: PartyAddressPayload = {
      type: form.type,
      street: form.street.trim(),
      city: form.city.trim() || undefined,
      province: form.province.trim() || undefined,
      postalCode: form.postalCode.trim() || undefined,
      country: form.country.trim() || undefined,
      isPrimary: form.isPrimary,
    };
    const obs = form.id
      ? this.api.updatePartyAddress(party.id, form.id, payload)
      : this.api.addPartyAddress(party.id, payload);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success('Direccion guardada.');
        this.savingChild.set(false);
        this.closeAddressForm();
        this.refreshDetail();
      },
      error: err => {
        this.toast.error(err?.error?.message ?? 'No se pudo guardar.');
        this.savingChild.set(false);
      },
    });
  }

  removeAddress(addr: PartyAddressDto) {
    const party = this.detail();
    if (!party) return;
    if (!confirm(`Eliminar dirección "${addr.street ?? ''}"?`)) return;
    this.api.removePartyAddress(party.id, addr.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.toast.success('Direccion eliminada.'); this.refreshDetail(); },
        error: () => this.toast.error('No se pudo eliminar.'),
      });
  }

  markAddressPrimary(addr: PartyAddressDto) {
    const party = this.detail();
    if (!party) return;
    this.api.setPrimaryPartyAddress(party.id, addr.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.toast.success('Direccion marcada como principal.'); this.refreshDetail(); },
        error: () => this.toast.error('No se pudo actualizar.'),
      });
  }

  private refreshDetail() {
    const current = this.detail();
    if (!current) return;
    this.api.getParty(current.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: d => this.detail.set(d) });
  }

  save() {
    if (!this.form.displayName.trim()) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }
    this.saving.set(true);

    const payload = {
      partyType: this.form.partyType as 'PERSON' | 'ORGANIZATION',
      displayName: this.form.displayName.trim(),
      legalName: this.form.legalName.trim() || undefined,
      firstName: this.form.firstName.trim() || undefined,
      lastName: this.form.lastName.trim() || undefined,
      email: this.form.email.trim() || undefined,
      phone: this.form.phone.trim() || undefined,
      mobile: this.form.mobile.trim() || undefined,
      notes: this.form.notes.trim() || undefined,
      identifierType: this.form.identifierType || undefined,
      identifierValue: this.form.identifierValue.trim() || undefined,
      street: this.form.street.trim() || undefined,
      city: this.form.city.trim() || undefined,
      province: this.form.province.trim() || undefined,
      roles: this.form.roles.length ? this.form.roles : undefined,
    };

    const obs = this.editing()
      ? this.api.updateParty(this.editId()!, payload)
      : this.api.createParty(payload);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success(this.editing() ? 'Contacto actualizado.' : 'Contacto creado.');
        this.saving.set(false);
        this.closeModal();
        this.reload();
        this.loadCounts();
      },
      error: () => {
        this.toast.error('Error al guardar.');
        this.saving.set(false);
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────
  private fillEditForm(d: PartyDetailDto) {
    this.editing.set(true);
    this.editId.set(d.id);
    this.form.partyType = d.partyType;
    this.form.displayName = d.displayName;
    this.form.legalName = d.legalName ?? '';
    this.form.firstName = d.firstName ?? '';
    this.form.lastName = d.lastName ?? '';
    this.form.email = d.email ?? '';
    this.form.phone = d.phone ?? '';
    this.form.mobile = d.mobile ?? '';
    this.form.notes = d.notes ?? '';
    this.form.identifierType = d.identifiers?.[0]?.type ?? '';
    this.form.identifierValue = d.identifiers?.[0]?.value ?? '';
    this.form.street = d.addresses?.[0]?.street ?? '';
    this.form.city = d.addresses?.[0]?.city ?? '';
    this.form.province = d.addresses?.[0]?.province ?? '';
    this.form.roles = [...d.roles];
    this.showModal.set(true);
  }

  private resetForm() {
    this.form.partyType = 'PERSON';
    this.form.displayName = '';
    this.form.legalName = '';
    this.form.firstName = '';
    this.form.lastName = '';
    this.form.email = '';
    this.form.phone = '';
    this.form.mobile = '';
    this.form.notes = '';
    this.form.identifierType = '';
    this.form.identifierValue = '';
    this.form.street = '';
    this.form.city = '';
    this.form.province = '';
    this.form.roles = [];
  }
}
