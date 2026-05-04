import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ErpApi, ErpSequenceDto, BranchDto } from '../../core/services/erp-api';
import { ToastService } from '../../core/services/toast';

@Component({
  selector: 'app-sequences',
  imports: [FormsModule],
  template: `
    <section class="page">

      <header class="page__header">
        <div>
          <span class="eyebrow">Plataforma</span>
          <h1>Secuencias</h1>
          <p>Configura la numeración automática de documentos internos por sucursal.</p>
        </div>
        <div class="page__acts">
          <button class="btn-primary" (click)="openCreate()">+ Nueva secuencia</button>
        </div>
      </header>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Codigo</th>
              <th>Nombre</th>
              <th>Sucursal</th>
              <th>Prefijo</th>
              <th>Num. actual</th>
              <th>Relleno</th>
              <th>Reset</th>
              <th>Vista previa</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            @for (seq of items(); track seq.id) {
              <tr>
                <td><code>{{ seq.code }}</code></td>
                <td>{{ seq.name }}</td>
                <td>{{ seq.branchName || 'Global' }}</td>
                <td>{{ seq.prefix || '-' }}</td>
                <td>{{ seq.currentNumber }}</td>
                <td>{{ seq.padLength }}</td>
                <td><span class="badge badge--muted">{{ resetLabel(seq.resetPolicy) }}</span></td>
                <td><code>{{ preview(seq) }}</code></td>
                <td>
                  <span class="badge" [class.badge--ok]="seq.active" [class.badge--off]="!seq.active">
                    {{ seq.active ? 'Activa' : 'Inactiva' }}
                  </span>
                </td>
                <td>
                  <button class="btn-sm btn-ghost" (click)="openEdit(seq)">Editar</button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="10" class="empty">No hay secuencias configuradas</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      @if (showModal()) {
        <div class="backdrop" (click)="closeModal()"></div>
        <div class="modal">
          <h2>{{ editing() ? 'Editar secuencia' : 'Nueva secuencia' }}</h2>

          @if (!editing()) {
            <label class="field">
              <span>Codigo *</span>
              <input type="text" [(ngModel)]="formCode" placeholder="PURCHASE_ORDER" />
            </label>
          }

          <label class="field">
            <span>Nombre *</span>
            <input type="text" [(ngModel)]="formName" />
          </label>

          @if (!editing()) {
            <label class="field">
              <span>Sucursal</span>
              <select [(ngModel)]="formBranchId">
                <option [ngValue]="null">Global (todas)</option>
                @for (b of branches(); track b.id) {
                  <option [ngValue]="b.id">{{ b.name }}</option>
                }
              </select>
            </label>
          }

          <div class="field-row">
            <label class="field">
              <span>Prefijo</span>
              <input type="text" [(ngModel)]="formPrefix" placeholder="OC-" />
            </label>
            <label class="field">
              <span>Sufijo</span>
              <input type="text" [(ngModel)]="formSuffix" />
            </label>
          </div>

          <div class="field-row">
            <label class="field">
              <span>Relleno (digitos)</span>
              <input type="number" [(ngModel)]="formPadLength" min="1" max="15" />
            </label>
            <label class="field">
              <span>Incremento</span>
              <input type="number" [(ngModel)]="formIncrement" min="1" />
            </label>
          </div>

          <label class="field">
            <span>Politica de reset</span>
            <select [(ngModel)]="formResetPolicy">
              <option value="NEVER">Nunca</option>
              <option value="YEARLY">Anual</option>
              <option value="MONTHLY">Mensual</option>
            </select>
          </label>

          @if (editing()) {
            <label class="field-check">
              <input type="checkbox" [(ngModel)]="formActive" />
              <span>Activa</span>
            </label>
          }

          <div class="modal__acts">
            <button class="btn-ghost" (click)="closeModal()">Cancelar</button>
            <button class="btn-primary" (click)="save()" [disabled]="saving()">
              {{ editing() ? 'Guardar' : 'Crear' }}
            </button>
          </div>
        </div>
      }
    </section>
  `,
  styles: `
    .page { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }
    .page__header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
    .page__header h1 { font-family: var(--ff-display, 'Cormorant Garamond', serif); font-size: 1.75rem; color: var(--c-text); margin: 0; }
    .page__header p { color: var(--c-text-muted); margin: .25rem 0 0; font-size: .92rem; }
    .eyebrow { font-size: .75rem; text-transform: uppercase; letter-spacing: .08em; color: var(--c-aurora); font-weight: 600; }

    .btn-primary { background: var(--c-aurora); color: #fff; border: none; padding: .55rem 1.25rem; border-radius: .5rem; font-weight: 600; cursor: pointer; }
    .btn-primary:disabled { opacity: .5; cursor: not-allowed; }
    .btn-sm { font-size: .8rem; padding: .3rem .75rem; border-radius: .5rem; border: none; cursor: pointer; font-weight: 600; }
    .btn-ghost { background: transparent; border: 1px solid var(--c-border); padding: .5rem 1.25rem; border-radius: .5rem; cursor: pointer; font-weight: 600; color: var(--c-text); }

    .table-wrap { background: var(--c-panel); border: 1px solid var(--c-border); border-radius: .75rem; overflow-x: auto; }
    .table { width: 100%; border-collapse: collapse; font-size: .9rem; }
    .table th { text-align: left; padding: .75rem 1rem; font-weight: 600; color: var(--c-text-muted); border-bottom: 1px solid var(--c-border); font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; }
    .table td { padding: .65rem 1rem; border-bottom: 1px solid var(--c-border); color: var(--c-text); }
    .table tbody tr:last-child td { border-bottom: none; }
    .empty { text-align: center; padding: 2rem !important; color: var(--c-text-muted); }
    code { background: color-mix(in srgb, var(--c-aurora) 10%, transparent); color: var(--c-text); padding: .15rem .4rem; border-radius: .35rem; font-size: .85rem; }

    .badge { font-size: .75rem; padding: .2rem .6rem; border-radius: 999px; font-weight: 600; }
    .badge--muted { background: color-mix(in srgb, var(--c-aurora) 8%, transparent); color: var(--c-text-muted); }
    .badge--ok { background: color-mix(in srgb, var(--c-ok) 14%, transparent); color: var(--c-ok); }
    .badge--off { background: color-mix(in srgb, var(--c-danger) 10%, transparent); color: var(--c-text-muted); }

    .backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.35); z-index: 900; }
    .modal { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: var(--c-panel); border: 1px solid var(--c-border); border-radius: 1rem; padding: 2rem; min-width: 460px; max-width: 560px; z-index: 910; box-shadow: var(--shadow-md); display: flex; flex-direction: column; gap: 1rem; }
    .modal h2 { font-family: var(--ff-display, 'Cormorant Garamond', serif); font-size: 1.25rem; margin: 0; color: var(--c-text); }
    .modal__acts { display: flex; justify-content: flex-end; gap: .75rem; margin-top: .5rem; }

    .field { display: flex; flex-direction: column; gap: .35rem; }
    .field span { font-size: .85rem; font-weight: 600; color: var(--c-text); }
    .field input, .field select { border: 1px solid var(--c-border); border-radius: .5rem; padding: .5rem .75rem; font-size: .9rem; background: var(--c-bg); color: var(--c-text); }
    .field-row { display: flex; gap: 1rem; }
    .field-row .field { flex: 1; }
    .field-check { display: flex; align-items: center; gap: .5rem; }
    .field-check input { width: 1.1rem; height: 1.1rem; }

    @media (max-width: 720px) {
      .modal {
        min-width: min(92vw, 460px);
        padding: 1.25rem;
      }

      .field-row {
        flex-direction: column;
      }
    }
  `,
})
export class Sequences implements OnInit {
  private readonly api = inject(ErpApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<ErpSequenceDto[]>([]);
  readonly branches = signal<BranchDto[]>([]);
  readonly showModal = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  private editId: number | null = null;

  formCode = '';
  formName = '';
  formBranchId: number | null = null;
  formPrefix = '';
  formSuffix = '';
  formPadLength = 7;
  formIncrement = 1;
  formResetPolicy = 'NEVER';
  formActive = true;

  ngOnInit(): void {
    this.load();
    this.loadBranches();
  }

  load(): void {
    this.api.getErpSequences()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => this.items.set(data),
        error: () => this.toast.error('Error al cargar secuencias'),
      });
  }

  loadBranches(): void {
    this.api.getAllBranches()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (data) => this.branches.set(data) });
  }

  openCreate(): void {
    this.editing.set(false);
    this.editId = null;
    this.formCode = '';
    this.formName = '';
    this.formBranchId = null;
    this.formPrefix = '';
    this.formSuffix = '';
    this.formPadLength = 7;
    this.formIncrement = 1;
    this.formResetPolicy = 'NEVER';
    this.formActive = true;
    this.showModal.set(true);
  }

  openEdit(seq: ErpSequenceDto): void {
    this.editing.set(true);
    this.editId = seq.id;
    this.formCode = seq.code;
    this.formName = seq.name;
    this.formBranchId = seq.branchId ?? null;
    this.formPrefix = seq.prefix || '';
    this.formSuffix = seq.suffix || '';
    this.formPadLength = seq.padLength;
    this.formIncrement = seq.incrementStep;
    this.formResetPolicy = seq.resetPolicy || 'NEVER';
    this.formActive = seq.active;
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  save(): void {
    if (!this.formName.trim()) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }

    this.saving.set(true);
    const obs = this.editing()
      ? this.api.updateErpSequence(this.editId!, {
          name: this.formName,
          prefix: this.formPrefix || undefined,
          suffix: this.formSuffix || undefined,
          padLength: this.formPadLength,
          incrementStep: this.formIncrement,
          resetPolicy: this.formResetPolicy,
          active: this.formActive,
        })
      : this.api.createErpSequence({
          code: this.formCode.trim().toUpperCase(),
          name: this.formName,
          branchId: this.formBranchId ?? undefined,
          prefix: this.formPrefix || undefined,
          suffix: this.formSuffix || undefined,
          padLength: this.formPadLength,
          incrementStep: this.formIncrement,
          resetPolicy: this.formResetPolicy,
        });

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toast.success(this.editing() ? 'Secuencia actualizada.' : 'Secuencia creada.');
        this.closeModal();
        this.load();
        this.saving.set(false);
      },
      error: () => {
        this.toast.error('Error al guardar.');
        this.saving.set(false);
      },
    });
  }

  preview(seq: ErpSequenceDto): string {
    const next = seq.currentNumber + seq.incrementStep;
    const padded = String(next).padStart(seq.padLength, '0');
    return (seq.prefix || '') + padded + (seq.suffix || '');
  }

  resetLabel(policy?: string): string {
    const map: Record<string, string> = { NEVER: 'Nunca', YEARLY: 'Anual', MONTHLY: 'Mensual' };
    return map[policy || 'NEVER'] || policy || 'Nunca';
  }
}
