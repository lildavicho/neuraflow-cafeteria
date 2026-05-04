import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiFeedback } from '../../core/models/ui-feedback';
import { HttpFeedback } from '../../core/services/http-feedback';
import { FileDownloadService } from '../../core/services/file-download';
import {
  AccountDto,
  JournalEntryDto,
  PayableDto,
  ReceivableDto,
  TaxRuleDto,
  ErpApi,
  ExportFormat,
} from '../../core/services/erp-api';
import { RequestFeedback } from '../shared/components/request-feedback';
import { ExportActions } from '../shared/components/export-actions';
import { PromptModal } from '../shared/components/prompt-modal';
import { taxRuleLabel } from '../../core/labels';

@Component({
  selector: 'app-accounting',
  imports: [FormsModule, CurrencyPipe, DatePipe, RequestFeedback, ExportActions, PromptModal],
  template: `
    <section class="page">
      <header class="page__header">
        <div>
          <span class="page__eyebrow">Caja y finanzas</span>
          <h1>Entiende ingresos, gastos y compromisos sin jerga contable</h1>
          <p>Revisa por cobrar, por pagar, tributos y movimientos recientes con lenguaje claro para la operación del negocio.</p>
        </div>
        <div class="page__actions">
          <small>{{ journalEntries().length }} movimientos visibles</small>
          <button type="button" class="page__button" [disabled]="loading()" (click)="reload()">Recargar</button>
          <app-export-actions
            [disabled]="loading()"
            [exporting]="exporting()"
            (exportRequested)="exportAccounting($event)"
          />
        </div>
      </header>

      @if (feedback(); as notice) {
        <app-request-feedback
          [tone]="notice.tone"
          [message]="notice.message"
          [traceId]="notice.traceId"
        />
      }

      <div class="stats-grid">
        <article class="stat-card">
          <span>Por cobrar</span>
          <strong>{{ totalReceivablesBalance() | currency: 'USD' : 'symbol' : '1.2-2' }}</strong>
          <small>{{ openReceivables() }} cuentas abiertas</small>
        </article>
        <article class="stat-card">
          <span>Por pagar</span>
          <strong>{{ totalPayablesBalance() | currency: 'USD' : 'symbol' : '1.2-2' }}</strong>
          <small>{{ openPayables() }} compromisos abiertos</small>
        </article>
        <article class="stat-card">
          <span>Cuentas</span>
          <strong>{{ accounts().length }}</strong>
          <small>rubros contables disponibles</small>
        </article>
        <article class="stat-card">
          <span>Reglas tributarias</span>
          <strong>{{ taxRules().length }}</strong>
          <small>impuestos activos para operar</small>
        </article>
      </div>

      <div class="layout">
        <section class="card">
          <header class="card__header">
            <div>
              <span class="page__eyebrow">Nuevo cobro</span>
              <h2>Registrar valor pendiente</h2>
            </div>
          </header>

          <div class="form-grid">
            <input [(ngModel)]="receivableDescription" class="field" placeholder="Descripción" />
            <input [(ngModel)]="receivableReference" class="field" placeholder="Referencia" />
            <input [(ngModel)]="receivableAmount" type="number" min="0.01" step="0.01" class="field" placeholder="Monto" />
            <input [(ngModel)]="receivableDueDate" type="date" class="field" />
          </div>

          <button type="button" class="page__button" [disabled]="loading()" (click)="createManualReceivable()">
            Guardar cuenta por cobrar
          </button>
        </section>

        <section class="card">
          <header class="card__header">
            <div>
              <span class="page__eyebrow">Resumen</span>
              <h2>Panorama financiero</h2>
            </div>
          </header>

          <div class="summary-grid">
            <article class="summary-card">
              <span>Por cobrar</span>
              <strong>{{ openReceivables() }}</strong>
            </article>
            <article class="summary-card">
              <span>Por pagar</span>
              <strong>{{ openPayables() }}</strong>
            </article>
            <article class="summary-card">
              <span>Última prioridad</span>
              <strong>{{ nextPriority() }}</strong>
            </article>
          </div>

          <div class="chips">
            @for (account of accounts().slice(0, 6); track account.id) {
              <span class="chip">{{ account.accountCode }} · {{ account.accountName }}</span>
            }
          </div>

          <div class="chips">
            @for (tax of taxRules(); track tax.id) {
              <span class="chip chip--accent" [title]="tax.taxCode + ' (' + tax.rate + '%)'">{{ taxLabel(tax.taxCode) }} · {{ tax.rate }}%</span>
            }
          </div>
        </section>
      </div>

      <section class="card">
        <header class="card__header">
          <div>
            <span class="page__eyebrow">Entradas pendientes</span>
            <h2>Valores por cobrar</h2>
          </div>
        </header>

        @if (loading()) {
          <p class="page__state">Consultando valores por cobrar...</p>
        } @else if (!receivables().length) {
          <p class="page__state">No hay valores por cobrar registrados.</p>
        } @else {
          <div class="table">
            <div class="table__head">
              <span>Documento</span>
              <span>Estado</span>
              <span>Total</span>
              <span>Saldo</span>
              <span>Acción</span>
            </div>
            @for (receivable of receivables(); track receivable.id) {
              <div class="table__row">
                <span>{{ receivable.sourceDocumentType }} #{{ receivable.sourceDocumentId }}</span>
                <span>{{ receivable.status }}</span>
                <span>{{ receivable.totalAmount | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                <span>{{ receivable.balance | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                <div>
                  @if (receivable.balance > 0) {
                    <button type="button" class="ghost" (click)="collectReceivable(receivable)">Registrar cobro</button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </section>

      <section class="card">
        <header class="card__header">
          <div>
            <span class="page__eyebrow">Salidas pendientes</span>
            <h2>Valores por pagar</h2>
          </div>
        </header>

        @if (loading()) {
          <p class="page__state">Consultando valores por pagar...</p>
        } @else if (!payables().length) {
          <p class="page__state">No hay valores por pagar registrados.</p>
        } @else {
          <div class="table">
            <div class="table__head">
              <span>Documento</span>
              <span>Estado</span>
              <span>Total</span>
              <span>Saldo</span>
              <span>Acción</span>
            </div>
            @for (payable of payables(); track payable.id) {
              <div class="table__row">
                <span>{{ payable.sourceDocumentType }} #{{ payable.sourceDocumentId }}</span>
                <span>{{ payable.status }}</span>
                <span>{{ payable.totalAmount | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                <span>{{ payable.balance | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                <div>
                  @if (payable.balance > 0) {
                    <button type="button" class="ghost" (click)="payPayable(payable)">Registrar pago</button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </section>

      <section class="card">
        <header class="card__header">
          <div>
            <span class="page__eyebrow">Actividad reciente</span>
            <h2>Movimientos registrados</h2>
          </div>
        </header>

        @if (loading()) {
          <p class="page__state">Consultando movimientos financieros...</p>
        } @else if (!journalEntries().length) {
          <p class="page__state">Aún no hay movimientos recientes para mostrar.</p>
        } @else {
          <div class="entries">
            @for (entry of journalEntries(); track entry.id) {
              <article class="entry-card">
                <header>
                  <strong>{{ entry.entryNumber }}</strong>
                  <span>{{ entry.description }}</span>
                </header>
                <small>{{ entry.entryDate | date: 'mediumDate' }} · {{ entry.sourceModule }}</small>
                <div class="entry-lines">
                  @for (line of entry.lines; track line.lineNumber) {
                    <div class="entry-line">
                      <span>L{{ line.lineNumber }} · Cuenta {{ line.accountId }}</span>
                      <span>Debe {{ line.debit | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                      <span>Haber {{ line.credit | currency: 'USD' : 'symbol' : '1.2-2' }}</span>
                    </div>
                  }
                </div>
              </article>
            }
          </div>
        }
      </section>
    </section>

    <app-prompt-modal
      [open]="promptOpen()"
      [title]="promptTitle()"
      [label]="promptStep() === 'amount' ? 'Monto' : 'Referencia'"
      [type]="promptStep() === 'amount' ? 'number' : 'text'"
      [initialValue]="promptStep() === 'amount' ? (promptTarget()?.balance ?? null) : null"
      [min]="0.01"
      step="0.01"
      [required]="promptStep() === 'amount'"
      [confirmLabel]="promptStep() === 'amount' ? 'Siguiente' : 'Confirmar'"
      (confirm)="onPromptConfirm($event)"
      (cancel)="onPromptCancel()"
    />
  `,
  styles: `
    :host { display: contents; }

    .page { display: grid; gap: 24px; padding: 32px; }

    .page__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      padding-bottom: 18px;
      border-bottom: 1px solid var(--line-subtle, #e8eaf0);
      scroll-margin-top: 24px;
    }

    .page__eyebrow {
      display: block;
      font-size: 10px;
      letter-spacing: 0.22em;
      text-transform: uppercase;
      font-weight: 500;
      color: var(--aurora);
      margin-bottom: 8px;
    }

    .page__header h1 {
      font-family: 'Cormorant Garamond', Georgia, serif;
      font-size: 26px;
      font-weight: 400;
      color: var(--text-strong);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .page__header p { font-size: 13px; font-weight: 300; color: var(--text-muted); margin: 0; line-height: 1.6; }

    .page__actions { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
    .page__actions small { font-size: 11px; color: var(--text-faint); }

    .page__button {
      padding: 10px 20px;
      background: transparent;
      border: 1px solid var(--aurora-border);
      color: var(--aurora);
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      border-radius: 2px;
      cursor: pointer;
      transition: background 200ms ease, border-color 200ms ease;
    }

    .page__button:hover:not(:disabled) { background: var(--aurora-ghost); border-color: var(--aurora); }
    .page__button:disabled { opacity: 0.35; cursor: not-allowed; }

    .ghost {
      padding: 7px 14px;
      background: transparent;
      border: 1px solid var(--line);
      color: var(--text-muted);
      font-size: 11px;
      border-radius: 2px;
      cursor: pointer;
      transition: border-color 150ms ease;
    }

    .ghost:hover { border-color: var(--line-strong); }

    .field {
      width: 100%;
      background: transparent;
      border: none;
      border-bottom: 1px solid var(--line);
      color: var(--text);
      padding: 9px 0;
      font-size: 13px;
      font-weight: 300;
      outline: none;
      transition: border-color 200ms ease;
    }

    .field::placeholder { color: var(--text-faint); }
    .field:focus { border-bottom-color: var(--aurora-border); }

    .page__state {
      margin: 0;
      padding: 11px 14px;
      border-radius: 3px;
      border: 1px solid var(--line-subtle);
      background: var(--surface-muted);
      font-size: 12.5px;
      color: var(--text-muted);
    }

    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 10px; }

    .stat-card { padding: 18px 20px; background: var(--bg-panel); border: 1px solid var(--line); border-radius: 4px; }
    .stat-card > span { display: block; font-size: 10px; letter-spacing: 0.18em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 8px; }
    .stat-card > strong { display: block; font-family: 'Cormorant Garamond', Georgia, serif; font-size: 26px; font-weight: 400; color: var(--text-strong); letter-spacing: -0.02em; margin-bottom: 3px; }
    .stat-card > small { font-size: 11px; font-weight: 300; color: var(--text-muted); }

    .layout { display: grid; grid-template-columns: 1fr minmax(300px, 0.9fr); gap: 12px; }

    .card { padding: 20px; background: var(--bg-panel); border: 1px solid var(--line); border-radius: 4px; display: grid; gap: 16px; }
    .card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .card h2 { font-size: 13px; font-weight: 500; color: var(--text-strong); margin: 0; }

    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

    .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }

    .summary-card { padding: 12px 14px; background: var(--surface); border: 1px solid var(--line); border-radius: 3px; }
    .summary-card span { display: block; font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-faint); margin-bottom: 6px; }
    .summary-card strong { display: block; font-family: 'Cormorant Garamond', Georgia, serif; font-size: 20px; font-weight: 400; color: var(--text-strong); }

    .chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .chip { padding: 4px 10px; border-radius: 2px; font-size: 11px; border: 1px solid var(--line); color: var(--text-muted); background: var(--surface-muted); }
    .chip--accent { border-color: var(--aurora-border); color: var(--aurora); background: var(--aurora-ghost); }

    .table { display: grid; gap: 4px; }
    .table__head, .table__row { display: grid; grid-template-columns: 1.4fr 0.8fr 0.8fr 0.8fr 1fr; gap: 12px; align-items: center; padding: 10px 12px; }
    .table__head { font-size: 10px; text-transform: uppercase; letter-spacing: 0.18em; font-weight: 500; color: var(--text-faint); }
    .table__row { background: var(--surface-muted); border-radius: 3px; font-size: 12.5px; color: var(--text-muted); }

    .entries { display: grid; gap: 6px; }
    .entry-card { padding: 14px; background: var(--surface-muted); border: 1px solid var(--line-subtle); border-radius: 3px; display: grid; gap: 8px; }
    .entry-card header { display: flex; justify-content: space-between; gap: 12px; }
    .entry-card header strong { font-size: 12.5px; color: var(--text); font-weight: 400; }
    .entry-card header span { font-size: 12px; color: var(--text-muted); }
    .entry-card small { font-size: 11px; color: var(--text-faint); }
    .entry-lines { display: grid; gap: 4px; }
    .entry-line { display: grid; grid-template-columns: 1.6fr 1fr 1fr; gap: 12px; font-size: 11.5px; color: var(--text-muted); }

    @media (max-width: 1100px) {
      .page { padding: 20px 16px; }
      .page__header { flex-direction: column; }
      .page__actions { align-items: flex-start; }
      .layout, .form-grid, .summary-grid, .table__head, .table__row, .entry-line { grid-template-columns: 1fr; }
    }
  `,
})
export class Accounting {
  private readonly erpApi = inject(ErpApi);
  private readonly httpFeedback = inject(HttpFeedback);
  private readonly fileDownload = inject(FileDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly accounts = signal<AccountDto[]>([]);
  protected readonly taxRules = signal<TaxRuleDto[]>([]);
  protected readonly journalEntries = signal<JournalEntryDto[]>([]);
  protected readonly receivables = signal<ReceivableDto[]>([]);
  protected readonly payables = signal<PayableDto[]>([]);
  protected readonly feedback = signal<UiFeedback | null>(null);
  protected readonly accountingLoading = signal(false);
  protected readonly exporting = signal<ExportFormat | null>(null);

  protected readonly promptOpen = signal<boolean>(false);
  protected readonly promptStep = signal<'amount' | 'reference'>('amount');
  protected readonly promptTarget = signal<{ kind: 'receivable' | 'payable'; id: number; balance: number } | null>(null);
  protected readonly promptAmount = signal<number>(0);

  protected receivableDescription = '';
  protected receivableReference = '';
  protected receivableAmount = 0;
  protected receivableDueDate = '';

  constructor() {
    this.reload();
  }

  protected loading(): boolean {
    return this.accountingLoading();
  }

  protected totalReceivablesBalance(): number {
    return this.receivables().reduce((sum, item) => sum + Number(item.balance || 0), 0);
  }

  protected totalPayablesBalance(): number {
    return this.payables().reduce((sum, item) => sum + Number(item.balance || 0), 0);
  }

  protected nextPriority(): string {
    if (this.openReceivables() > 0) {
      return 'Cobrar';
    }
    if (this.openPayables() > 0) {
      return 'Pagar';
    }
    return 'En orden';
  }

  protected taxLabel(code: string | null | undefined): string {
    return taxRuleLabel(code);
  }

  protected exportAccounting(format: ExportFormat): void {
    this.exporting.set(format);
    this.feedback.set(null);

    this.erpApi
      .exportAccounting(format)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.fileDownload.download(response, `panorama-contable.${format}`);
          this.feedback.set(this.httpFeedback.success(`Panorama contable exportado en ${format.toUpperCase()}.`));
          this.exporting.set(null);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo exportar contabilidad.'));
          this.exporting.set(null);
        },
      });
  }

  protected reload(): void {
    this.accountingLoading.set(true);

    this.erpApi
      .getAccounts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => this.accounts.set(items ?? []),
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar el catálogo de cuentas.'));
        },
      });

    this.erpApi
      .getTaxRules()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => this.taxRules.set(items ?? []),
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar las reglas tributarias.'));
        },
      });

    this.erpApi
      .getJournalEntries()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => this.journalEntries.set(items ?? []),
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo cargar la actividad reciente.'));
        },
      });

    this.erpApi
      .getReceivables()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => this.receivables.set(items ?? []),
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar los valores por cobrar.'));
        },
      });

    this.erpApi
      .getPayables()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.payables.set(items ?? []);
          this.accountingLoading.set(false);
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudieron cargar los valores por pagar.'));
          this.accountingLoading.set(false);
        },
      });
  }

  protected createManualReceivable(): void {
    this.feedback.set(null);
    if (!this.receivableDescription.trim() || this.receivableAmount <= 0) {
      this.feedback.set(this.httpFeedback.warning('Ingresa descripción y monto válido para registrar el valor pendiente.'));
      return;
    }

    this.erpApi
      .createManualReceivable({
        description: this.receivableDescription.trim(),
        externalReference: this.receivableReference || undefined,
        dueDate: this.receivableDueDate || undefined,
        totalAmount: this.receivableAmount,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.feedback.set(this.httpFeedback.success('Valor por cobrar registrado.'));
          this.receivableDescription = '';
          this.receivableReference = '';
          this.receivableAmount = 0;
          this.receivableDueDate = '';
          this.reload();
        },
        error: (error) => {
          this.feedback.set(this.httpFeedback.fromError(error, 'No se pudo registrar el valor por cobrar.'));
        },
      });
  }

  protected collectReceivable(receivable: ReceivableDto): void {
    this.promptTarget.set({ kind: 'receivable', id: receivable.id, balance: Number(receivable.balance || 0) });
    this.promptStep.set('amount');
    this.promptOpen.set(true);
  }

  protected payPayable(payable: PayableDto): void {
    this.promptTarget.set({ kind: 'payable', id: payable.id, balance: Number(payable.balance || 0) });
    this.promptStep.set('amount');
    this.promptOpen.set(true);
  }

  protected onPromptConfirm(value: string): void {
    const target = this.promptTarget();
    if (!target) { this.promptOpen.set(false); return; }

    if (this.promptStep() === 'amount') {
      const amount = Number(value);
      if (!amount || amount <= 0) { this.promptOpen.set(false); return; }
      this.promptAmount.set(amount);
      this.promptStep.set('reference');
      return;
    }

    const amount = this.promptAmount();
    const reference = value.trim() || undefined;
    this.promptOpen.set(false);

    const label = target.kind === 'receivable' ? 'Cobro' : 'Pago';
    const fallback = target.kind === 'receivable' ? 'No se pudo aplicar el cobro.' : 'No se pudo aplicar el pago.';
    const onSuccess = () => {
      this.feedback.set(this.httpFeedback.success(`${label} aplicado al registro #${target.id}.`));
      this.reload();
    };
    const onError = (error: unknown) => {
      this.feedback.set(this.httpFeedback.fromError(error as HttpErrorResponse, fallback));
    };

    if (target.kind === 'receivable') {
      this.erpApi.applyReceivablePayment(target.id, { amount, reference })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.erpApi.applyPayablePayment(target.id, { amount, reference })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({ next: onSuccess, error: onError });
    }
  }

  protected onPromptCancel(): void {
    this.promptOpen.set(false);
    this.promptTarget.set(null);
  }

  protected promptTitle(): string {
    const target = this.promptTarget();
    if (!target) return '';
    const verb = target.kind === 'receivable' ? 'cobrar' : 'pagar';
    return this.promptStep() === 'amount'
      ? `Monto a ${verb} — registro #${target.id}`
      : `Referencia ${target.kind === 'receivable' ? 'del cobro' : 'del pago'}`;
  }

  protected openReceivables(): number {
    return this.receivables().filter((item) => item.balance > 0).length;
  }

  protected openPayables(): number {
    return this.payables().filter((item) => item.balance > 0).length;
  }
}


