import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ErpApi, ApprovalInstanceDto } from '../../core/services/erp-api';
import { ToastService } from '../../core/services/toast';
import { AuthSession } from '../../core/services/auth-session';
import { Paginator } from '../shared/components/paginator';

@Component({
  selector: 'app-approvals',
  imports: [DatePipe, FormsModule, Paginator],
  template: `
    <section class="page">

      <!-- ══ HEADER ═════════════════════════════════════════════════════ -->
      <header class="page__header">
        <div>
          <span class="eyebrow">Plataforma</span>
          <h1>Aprobaciones</h1>
          <p>Bandeja de solicitudes pendientes de aprobacion. Revisa, aprueba o rechaza cada solicitud.</p>
        </div>
      </header>

      <!-- ══ KPIs ═══════════════════════════════════════════════════════ -->
      <div class="kpi-row">
        <div class="kpi-card">
          <span class="kpi-value">{{ pendingCount() }}</span>
          <span class="kpi-label">Pendientes</span>
        </div>
      </div>

      <!-- ══ TABLE ══════════════════════════════════════════════════════ -->
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Flujo</th>
              <th>Tipo entidad</th>
              <th>ID Entidad</th>
              <th>Paso actual</th>
              <th>Estado</th>
              <th>Fecha</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>{{ item.id }}</td>
                <td>{{ item.definitionName || '-' }}</td>
                <td><span class="badge badge--muted">{{ entityLabel(item.entityType) }}</span></td>
                <td>{{ item.entityId }}</td>
                <td>Paso {{ item.currentStep }}</td>
                <td><span class="badge" [class]="statusClass(item.status)">{{ statusLabel(item.status) }}</span></td>
                <td>{{ item.createdAt | date:'short' }}</td>
                <td class="actions-cell">
                  @if (item.status === 'PENDING') {
                    <button class="btn-sm btn-ok" (click)="approve(item)" title="Aprobar">Aprobar</button>
                    <button class="btn-sm btn-danger" (click)="openReject(item)" title="Rechazar">Rechazar</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="8" class="empty">No hay solicitudes pendientes</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-paginator
        [page]="currentPage()"
        [pageSize]="pageSize"
        [totalElements]="totalElements()"
        (pageChange)="goToPage($event)" />

      <!-- ══ REJECT MODAL ═══════════════════════════════════════════════ -->
      @if (rejectTarget()) {
        <div class="backdrop" (click)="closeReject()"></div>
        <div class="modal">
          <h2>Rechazar solicitud #{{ rejectTarget()!.id }}</h2>
          <label class="field">
            <span>Motivo del rechazo</span>
            <textarea rows="3" [(ngModel)]="rejectComment"></textarea>
          </label>
          <div class="modal__acts">
            <button class="btn-ghost" (click)="closeReject()">Cancelar</button>
            <button class="btn-danger" (click)="confirmReject()" [disabled]="saving()">Rechazar</button>
          </div>
        </div>
      }
    </section>
  `,
  styles: `
    .page { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }
    .page__header { margin-bottom: 1.5rem; }
    .page__header h1 { font-family: var(--ff-display, 'Cormorant Garamond', serif); font-size: 1.75rem; color: var(--c-text); margin: 0; }
    .page__header p { color: var(--c-text-muted); margin: .25rem 0 0; font-size: .92rem; }
    .eyebrow { font-size: .75rem; text-transform: uppercase; letter-spacing: .08em; color: var(--c-aurora); font-weight: 600; }

    .kpi-row { display: flex; gap: 1rem; margin-bottom: 1.5rem; }
    .kpi-card { background: var(--c-panel); border: 1px solid var(--c-border); border-radius: .75rem; padding: 1.25rem 1.5rem; min-width: 140px; }
    .kpi-value { font-size: 1.75rem; font-weight: 700; color: var(--c-aurora); display: block; }
    .kpi-label { font-size: .8rem; color: var(--c-text-muted); }

    .table-wrap { background: var(--c-panel); border: 1px solid var(--c-border); border-radius: .75rem; overflow-x: auto; }
    .table { width: 100%; border-collapse: collapse; font-size: .9rem; }
    .table th { text-align: left; padding: .75rem 1rem; font-weight: 600; color: var(--c-text-muted); border-bottom: 1px solid var(--c-border); font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; }
    .table td { padding: .65rem 1rem; border-bottom: 1px solid var(--c-border); color: var(--c-text); }
    .table tbody tr:last-child td { border-bottom: none; }
    .empty { text-align: center; padding: 2rem !important; color: var(--c-text-muted); }

    .badge { font-size: .75rem; padding: .2rem .6rem; border-radius: 999px; font-weight: 600; }
    .badge--muted { background: color-mix(in srgb, var(--c-aurora) 8%, transparent); color: var(--c-text-muted); }
    .badge--pending { background: color-mix(in srgb, var(--c-warn) 14%, transparent); color: var(--c-warn); }
    .badge--approved { background: color-mix(in srgb, var(--c-ok) 14%, transparent); color: var(--c-ok); }
    .badge--rejected { background: color-mix(in srgb, var(--c-danger) 12%, transparent); color: var(--c-danger); }
    .badge--cancelled { background: color-mix(in srgb, var(--c-danger) 8%, transparent); color: var(--c-text-muted); }

    .actions-cell { display: flex; gap: .5rem; }
    .btn-sm { font-size: .8rem; padding: .3rem .75rem; border-radius: .5rem; border: none; cursor: pointer; font-weight: 600; transition: opacity var(--dur-fast, 120ms) var(--ease-out, ease-out); }
    .btn-sm:hover { opacity: .85; }
    .btn-ok { background: var(--c-ok); color: #fff; }
    .btn-danger { background: var(--c-danger); color: #fff; border: none; padding: .5rem 1.25rem; border-radius: .5rem; cursor: pointer; font-weight: 600; }

    .backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.35); z-index: 900; }
    .modal { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 1rem; padding: 2rem; min-width: 400px; max-width: 520px; z-index: 910; box-shadow: var(--shadow-md); }
    .modal h2 { font-family: var(--ff-display, 'Cormorant Garamond', serif); font-size: 1.25rem; margin: 0 0 1.25rem; }
    .modal__acts { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem; }
    .btn-ghost { background: transparent; border: 1px solid var(--c-border); padding: .5rem 1.25rem; border-radius: .5rem; cursor: pointer; font-weight: 600; color: var(--c-text); }

    .field { display: flex; flex-direction: column; gap: .35rem; }
    .field span { font-size: .85rem; font-weight: 600; color: var(--c-text); }
    .field textarea { border: 1px solid var(--c-border); border-radius: .5rem; padding: .5rem .75rem; font-size: .9rem; resize: vertical; background: var(--c-bg); color: var(--c-text); }

    @media (max-width: 720px) {
      .modal {
        min-width: min(92vw, 400px);
        padding: 1.25rem;
      }
    }
  `,
})
export class Approvals implements OnInit {
  private readonly api = inject(ErpApi);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthSession);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<ApprovalInstanceDto[]>([]);
  readonly pendingCount = signal(0);
  readonly currentPage = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly saving = signal(false);
  readonly rejectTarget = signal<ApprovalInstanceDto | null>(null);
  rejectComment = '';

  ngOnInit(): void {
    this.load();
    this.loadCounts();
  }

  load(page = 0): void {
    this.api.getApprovalsPending(page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.items.set(res.content);
          this.currentPage.set(page);
          this.totalElements.set(res.totalElements);
        },
        error: () => this.toast.error('Error al cargar aprobaciones'),
      });
  }

  loadCounts(): void {
    this.api.getApprovalCounts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (c) => this.pendingCount.set(c.pending),
      });
  }

  goToPage(page: number): void {
    this.load(page);
  }

  approve(item: ApprovalInstanceDto): void {
    const user = this.auth.user();
    this.saving.set(true);
    this.api.approveInstance(item.id, { userName: user?.fullName || user?.email || 'Usuario' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Solicitud aprobada');
          this.load(this.currentPage());
          this.loadCounts();
          this.saving.set(false);
        },
        error: () => {
          this.toast.error('Error al aprobar');
          this.saving.set(false);
        },
      });
  }

  openReject(item: ApprovalInstanceDto): void {
    this.rejectTarget.set(item);
    this.rejectComment = '';
  }

  closeReject(): void {
    this.rejectTarget.set(null);
  }

  confirmReject(): void {
    const target = this.rejectTarget();
    if (!target) return;
    const user = this.auth.user();
    this.saving.set(true);
    this.api.rejectInstance(target.id, {
      userName: user?.fullName || user?.email || 'Usuario',
      comment: this.rejectComment || undefined,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Solicitud rechazada');
          this.closeReject();
          this.load(this.currentPage());
          this.loadCounts();
          this.saving.set(false);
        },
        error: () => {
          this.toast.error('Error al rechazar');
          this.saving.set(false);
        },
      });
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendiente', APPROVED: 'Aprobada', REJECTED: 'Rechazada', CANCELLED: 'Cancelada',
    };
    return map[status] || status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge--pending', APPROVED: 'badge--approved',
      REJECTED: 'badge--rejected', CANCELLED: 'badge--cancelled',
    };
    return 'badge ' + (map[status] || 'badge--muted');
  }

  entityLabel(type: string): string {
    const map: Record<string, string> = {
      PURCHASE_ORDER: 'Orden de compra', CREDIT_NOTE: 'Nota de crédito',
      INVENTORY_ADJUST: 'Ajuste inventario', DISCOUNT: 'Descuento',
      EXPENSE: 'Gasto', OTHER: 'Otro',
    };
    return map[type] || type;
  }
}
