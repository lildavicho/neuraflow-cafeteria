import {
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ErpApi, BranchDto, WarehouseDto } from '../../core/services/erp-api';
import { ToastService } from '../../core/services/toast';
import { Paginator } from '../shared/components/paginator';

@Component({
  selector: 'app-warehouses',
  imports: [FormsModule, Paginator],
  template: `
    <section class="page">

      <!-- ══ HEADER ═════════════════════════════════════════════════════ -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Plataforma</span>
          <h1>Bodegas</h1>
          <p>Ubicaciones fisicas de almacenamiento por sucursal. Cada bodega gestiona su propio inventario.</p>
        </div>
        <div class="page__acts">
          <button class="btn-primary" (click)="openCreate()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:18px;height:18px">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
            </svg>
            Nueva bodega
          </button>
        </div>
      </header>

      <!-- ══ KPI STRIP ══════════════════════════════════════════════════ -->
      <div class="kpi-row">
        <div class="kpi">
          <span class="kpi__label">Total bodegas</span>
          <span class="kpi__val">{{ warehouses().length }}</span>
        </div>
        <div class="kpi">
          <span class="kpi__label">Activas</span>
          <span class="kpi__val">{{ warehouses().filter(w => w.active).length }}</span>
        </div>
      </div>

      <!-- ══ FILTERS ════════════════════════════════════════════════════ -->
      <section class="card">
        <header class="card__hd">
          <div><span class="eyebrow">Inventario</span><h2>Bodegas registradas</h2></div>
          <div class="tbl-controls">
            <select class="search-field" [ngModel]="branchFilter()" (ngModelChange)="branchFilter.set($event)">
              <option value="">Todas las sucursales</option>
              @for (b of allBranches(); track b.id) {
                <option [value]="b.id">{{ b.name }}</option>
              }
            </select>
            <input class="search-field" [(ngModel)]="search" placeholder="Buscar bodega..."/>
          </div>
        </header>

        @if (loading()) {
          <p class="state-msg">Cargando bodegas...</p>
        } @else if (errorMsg()) {
          <p class="state-msg state-msg--error">
            {{ errorMsg() }}
            <button class="link-btn" (click)="load()">Reintentar</button>
          </p>
        } @else if (!filtered().length) {
          <p class="state-msg">
            @if (warehouses().length === 0) {
              No hay bodegas registradas. Crea la primera.
            } @else {
              Sin resultados.
              <button class="link-btn" (click)="clearFilters()">Limpiar filtros</button>
            }
          </p>
        } @else {
          <div class="tbl">
            <div class="tbl__head">
              <span>Codigo</span>
              <span>Nombre</span>
              <span>Sucursal</span>
              <span>Tipo</span>
              <span>Estado</span>
              <span>Acciones</span>
            </div>

            @for (w of paged(); track w.id) {
              <div class="tbl__row">
                <span class="mono">{{ w.code }}</span>
                <span class="cell-name">{{ w.name }}</span>
                <span class="muted">{{ w.branchName ?? '—' }}</span>
                <span>
                  <span class="type-chip" [attr.data-type]="w.type">{{ typeLabel(w.type) }}</span>
                </span>
                <span>
                  <span class="badge" [class.badge--ok]="w.active" [class.badge--warn]="!w.active">
                    {{ w.active ? 'Activa' : 'Inactiva' }}
                  </span>
                </span>
                <div class="tbl__acts">
                  <button class="act-btn" (click)="openEdit(w)">Editar</button>
                  @if (w.active) {
                    <button class="act-btn act-btn--danger" (click)="confirmDeactivate(w)">Desactivar</button>
                  }
                </div>
              </div>
            }
          </div>

          <app-paginator
            [page]="page()"
            [pageSize]="pageSize"
            [totalElements]="filtered().length"
            (pageChange)="page.set($event)"
          />
        }
      </section>

      <!-- ══ MODAL: CREATE/EDIT ═════════════════════════════════════════ -->
      @if (showModal()) {
        <div class="modal-bd" (click)="closeModal()">
          <div class="modal-box" (click)="$event.stopPropagation()">
            <header class="modal__hd">
              <h3>{{ editing() ? 'Editar bodega' : 'Nueva bodega' }}</h3>
              <button class="modal__close" (click)="closeModal()">&times;</button>
            </header>
            <form class="modal__body" (ngSubmit)="save()">
              <div class="form-grid">
                <label class="field">
                  <span class="field__label">Sucursal *</span>
                  <select class="field__input" [(ngModel)]="form.branchId" name="branchId" required
                          [disabled]="editing()">
                    <option [ngValue]="null">Seleccionar...</option>
                    @for (b of allBranches(); track b.id) {
                      <option [ngValue]="b.id">{{ b.name }} ({{ b.code }})</option>
                    }
                  </select>
                </label>
                <label class="field">
                  <span class="field__label">Codigo *</span>
                  <input class="field__input" [(ngModel)]="form.code" name="code" required maxlength="20"
                         [disabled]="editing()" placeholder="Ej: BOD001"/>
                </label>
                <label class="field">
                  <span class="field__label">Nombre *</span>
                  <input class="field__input" [(ngModel)]="form.name" name="name" required maxlength="150"
                         placeholder="Ej: Bodega Principal"/>
                </label>
                <label class="field">
                  <span class="field__label">Tipo *</span>
                  <select class="field__input" [(ngModel)]="form.type" name="type" required>
                    <option value="STORE">Tienda</option>
                    <option value="WAREHOUSE">Almacen</option>
                    <option value="TRANSIT">Transito</option>
                  </select>
                </label>
                <label class="field" style="grid-column: 1/-1">
                  <span class="field__label">Direccion</span>
                  <input class="field__input" [(ngModel)]="form.address" name="address" maxlength="300"
                         placeholder="Direccion de la bodega"/>
                </label>
                @if (editing()) {
                  <label class="field field--check">
                    <input type="checkbox" [(ngModel)]="form.active" name="active"/>
                    <span class="field__label">Activa</span>
                  </label>
                }
              </div>
              <footer class="modal__ft">
                <button type="button" class="btn-outline" (click)="closeModal()">Cancelar</button>
                <button type="submit" class="btn-primary" [disabled]="saving()">
                  {{ saving() ? 'Guardando...' : (editing() ? 'Guardar cambios' : 'Crear bodega') }}
                </button>
              </footer>
            </form>
          </div>
        </div>
      }

      <!-- ══ CONFIRM DEACTIVATE ═════════════════════════════════════════ -->
      @if (confirmWarehouse()) {
        <div class="modal-bd" (click)="confirmWarehouse.set(null)">
          <div class="modal-box modal-box--sm" (click)="$event.stopPropagation()">
            <header class="modal__hd"><h3>Confirmar desactivacion</h3></header>
            <div class="modal__body">
              <p>Se desactivara la bodega <strong>{{ confirmWarehouse()!.name }}</strong>. El inventario asociado dejara de estar disponible para operaciones.</p>
            </div>
            <footer class="modal__ft">
              <button class="btn-outline" (click)="confirmWarehouse.set(null)">Cancelar</button>
              <button class="btn-danger" (click)="doDeactivate()">Desactivar</button>
            </footer>
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
    .kpi { flex: 1 1 180px; background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 12px; padding: 1rem 1.25rem; }
    .kpi__label { font-size: 0.75rem; color: var(--c-text-muted); text-transform: uppercase; letter-spacing: 0.06em; display: block; margin-bottom: 0.25rem; }
    .kpi__val { font-family: var(--ff-display); font-size: 1.5rem; color: var(--c-text); }

    .card { background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 12px; overflow: hidden; }
    .card__hd { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid var(--c-border); flex-wrap: wrap; gap: 0.75rem; }
    .card__hd h2 { font-family: var(--ff-display); font-size: 1.125rem; margin: 0.125rem 0 0; color: var(--c-text); }

    .tbl-controls { display: flex; gap: 0.5rem; align-items: center; }
    .search-field { padding: 0.5rem 0.75rem; border: 1px solid var(--c-border); border-radius: 8px; font-size: 0.8125rem; background: var(--c-bg); color: var(--c-text); }
    .search-field:focus { outline: none; border-color: var(--c-aurora); box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-aurora) 15%, transparent); }
    select.search-field { min-width: 180px; }

    .tbl { display: grid; }
    .tbl__head { display: grid; grid-template-columns: 100px 1fr 150px 120px 90px 140px; padding: 0.625rem 1.25rem; font-size: 0.6875rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--c-text-muted); border-bottom: 1px solid var(--c-border); }
    .tbl__row { display: grid; grid-template-columns: 100px 1fr 150px 120px 90px 140px; padding: 0.75rem 1.25rem; align-items: center; font-size: 0.8125rem; border-bottom: 1px solid var(--c-border-soft, var(--c-border)); transition: background var(--dur-fast) var(--ease-out); }
    .tbl__row:hover { background: color-mix(in srgb, var(--c-aurora) 4%, transparent); }
    .tbl__acts { display: flex; gap: 0.375rem; }

    .mono { font-family: 'DM Sans', monospace; font-size: 0.8125rem; letter-spacing: 0.02em; }
    .muted { color: var(--c-text-muted); }
    .cell-name { font-weight: 500; color: var(--c-text); }

    .badge { display: inline-block; padding: 0.125rem 0.5rem; border-radius: 6px; font-size: 0.6875rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; }
    .badge--ok { background: color-mix(in srgb, var(--c-ok) 12%, transparent); color: var(--c-ok); }
    .badge--warn { background: color-mix(in srgb, var(--c-warn) 12%, transparent); color: var(--c-warn); }

    .type-chip { display: inline-block; padding: 0.125rem 0.5rem; border-radius: 6px; font-size: 0.6875rem; font-weight: 600; background: color-mix(in srgb, var(--c-aurora) 10%, transparent); color: var(--c-aurora); text-transform: uppercase; letter-spacing: 0.04em; }

    .btn-primary { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.5rem 1rem; background: var(--c-aurora); color: #fff; border: none; border-radius: 8px; font-size: 0.8125rem; font-weight: 600; cursor: pointer; transition: opacity var(--dur-fast) var(--ease-out); }
    .btn-primary:hover { opacity: 0.9; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.5rem 1rem; background: transparent; color: var(--c-text); border: 1px solid var(--c-border); border-radius: 8px; font-size: 0.8125rem; font-weight: 500; cursor: pointer; }
    .btn-outline:hover { border-color: var(--c-aurora); color: var(--c-aurora); }
    .btn-danger { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.5rem 1rem; background: var(--c-danger); color: #fff; border: none; border-radius: 8px; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
    .act-btn { padding: 0.25rem 0.625rem; font-size: 0.75rem; border: 1px solid var(--c-border); border-radius: 6px; background: var(--c-panel); color: var(--c-text); cursor: pointer; }
    .act-btn:hover { border-color: var(--c-aurora); color: var(--c-aurora); }
    .act-btn--danger:hover { border-color: var(--c-danger); color: var(--c-danger); }

    .state-msg { padding: 3rem 1.25rem; text-align: center; color: var(--c-text-muted); font-size: 0.875rem; }
    .state-msg--error { color: var(--c-danger); }
    .link-btn { background: none; border: none; color: var(--c-aurora); font-size: inherit; cursor: pointer; text-decoration: underline; }

    .modal-bd { position: fixed; inset: 0; z-index: 900; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.45); backdrop-filter: blur(4px); animation: fadeIn var(--dur-fast) var(--ease-out); }
    .modal-box { background: var(--c-panel); border-radius: 16px; width: min(560px, 94vw); max-height: 90vh; overflow-y: auto; box-shadow: 0 24px 48px rgba(0,0,0,0.18); animation: slideUp var(--dur-normal) var(--ease-out); }
    .modal-box--sm { width: min(420px, 92vw); }
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
    .field__input:disabled { opacity: 0.5; }
    .field--check { flex-direction: row; align-items: center; gap: 0.5rem; padding-top: 0.5rem; }
    .field--check input[type="checkbox"] { accent-color: var(--c-aurora); width: 16px; height: 16px; }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

    @media (max-width: 768px) {
      .tbl__head, .tbl__row { grid-template-columns: 80px 1fr 100px 80px 100px; }
      .tbl__head > :nth-child(4), .tbl__row > :nth-child(4) { display: none; }
      .form-grid { grid-template-columns: 1fr; }
    }
  `,
})
export class Warehouses implements OnInit {
  private readonly api = inject(ErpApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ───────────────────────────────────────────────────────────
  warehouses = signal<WarehouseDto[]>([]);
  allBranches = signal<BranchDto[]>([]);
  loading = signal(true);
  errorMsg = signal('');
  search = signal('');
  branchFilter = signal('');
  page = signal(0);
  pageSize = 15;

  showModal = signal(false);
  editing = signal(false);
  editId = signal<number | null>(null);
  saving = signal(false);
  confirmWarehouse = signal<WarehouseDto | null>(null);

  form: {
    branchId: number | null;
    code: string;
    name: string;
    type: 'STORE' | 'WAREHOUSE' | 'TRANSIT';
    address: string;
    active: boolean;
  } = {
    branchId: null,
    code: '',
    name: '',
    type: 'STORE',
    address: '',
    active: true,
  };

  // ── Computed ────────────────────────────────────────────────────────
  filtered = computed(() => {
    let list = this.warehouses();
    const bf = this.branchFilter();
    if (bf) list = list.filter(w => String(w.branchId) === bf);
    const q = this.search().toLowerCase();
    if (q) list = list.filter(w => w.name.toLowerCase().includes(q) || w.code.toLowerCase().includes(q));
    return list;
  });

  paged = computed(() => {
    const start = this.page() * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  // ── Lifecycle ───────────────────────────────────────────────────────
  ngOnInit() {
    this.load();
    this.loadBranches();
  }

  load() {
    this.loading.set(true);
    this.errorMsg.set('');
    this.api.getAllWarehouses()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          this.warehouses.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.errorMsg.set('Error al cargar bodegas.');
          this.loading.set(false);
        },
      });
  }

  loadBranches() {
    this.api.getAllBranches()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: data => this.allBranches.set(data) });
  }

  clearFilters() {
    this.search.set('');
    this.branchFilter.set('');
  }

  typeLabel(type: string): string {
    switch (type) {
      case 'STORE': return 'Tienda';
      case 'WAREHOUSE': return 'Almacen';
      case 'TRANSIT': return 'Transito';
      default: return type;
    }
  }

  // ── Modal ───────────────────────────────────────────────────────────
  openCreate() {
    this.editing.set(false);
    this.editId.set(null);
    this.resetForm();
    this.showModal.set(true);
  }

  openEdit(w: WarehouseDto) {
    this.editing.set(true);
    this.editId.set(w.id);
    this.form.branchId = w.branchId;
    this.form.code = w.code;
    this.form.name = w.name;
    this.form.type = w.type;
    this.form.address = w.address ?? '';
    this.form.active = w.active;
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
  }

  save() {
    if (!this.form.branchId || !this.form.code.trim() || !this.form.name.trim()) {
      this.toast.warning('Sucursal, código y nombre son obligatorios.');
      return;
    }
    this.saving.set(true);

    const payload = {
      branchId: this.form.branchId,
      code: this.form.code.trim(),
      name: this.form.name.trim(),
      type: this.form.type,
      address: this.form.address.trim() || undefined,
    };

    const obs = this.editing()
      ? this.api.updateWarehouse(this.editId()!, { ...payload, active: this.form.active })
      : this.api.createWarehouse(payload);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success(this.editing() ? 'Bodega actualizada.' : 'Bodega creada.');
        this.saving.set(false);
        this.closeModal();
        this.load();
      },
      error: () => {
        this.toast.error('Error al guardar la bodega.');
        this.saving.set(false);
      },
    });
  }

  // ── Deactivate ──────────────────────────────────────────────────────
  confirmDeactivate(w: WarehouseDto) {
    this.confirmWarehouse.set(w);
  }

  doDeactivate() {
    const w = this.confirmWarehouse();
    if (!w) return;
    this.api.deactivateWarehouse(w.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Bodega desactivada.');
          this.confirmWarehouse.set(null);
          this.load();
        },
        error: () => this.toast.error('Error al desactivar.'),
      });
  }

  private resetForm() {
    this.form.branchId = null;
    this.form.code = '';
    this.form.name = '';
    this.form.type = 'STORE';
    this.form.address = '';
    this.form.active = true;
  }
}
