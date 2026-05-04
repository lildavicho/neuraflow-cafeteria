import { Component, input } from '@angular/core';

type ModuleMetric = {
  label: string;
  value: string;
  detail: string;
};

type ModuleAction = {
  label: string;
  detail: string;
};

type ModuleSection = {
  eyebrow: string;
  title: string;
  description: string;
  items: string[];
};

@Component({
  selector: 'app-module-page',
  template: `
    <section class="page module-page">
      <article class="surface surface--hero module-page__hero">
        <div class="module-page__copy">
          <span class="page__eyebrow">{{ eyebrow() }}</span>
          <h3>{{ title() }}</h3>
          <p>{{ summary() }}</p>
        </div>

        @if (actions().length) {
          <div class="action-grid">
            @for (action of actions(); track action.label) {
              <article class="action-card">
                <strong>{{ action.label }}</strong>
                <small>{{ action.detail }}</small>
              </article>
            }
          </div>
        }
      </article>

      @if (metrics().length) {
        <div class="metric-strip">
          @for (metric of metrics(); track metric.label) {
            <article class="metric-card">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <small>{{ metric.detail }}</small>
            </article>
          }
        </div>
      }

      @if (sections().length) {
        <div class="section-grid">
          @for (section of sections(); track section.title) {
            <article class="section-card">
              <span class="section-card__eyebrow">{{ section.eyebrow }}</span>
              <div>
                <h3>{{ section.title }}</h3>
                <p>{{ section.description }}</p>
              </div>
              <ul class="bullet-list">
                @for (item of section.items; track item) {
                  <li>{{ item }}</li>
                }
              </ul>
            </article>
          }
        </div>
      }

      @if (bullets().length) {
        <article class="surface module-page__footer">
          <span class="page__eyebrow">{{ footerLabel() }}</span>
          <ul class="bullet-list">
            @for (item of bullets(); track item) {
              <li>{{ item }}</li>
            }
          </ul>
        </article>
      }
    </section>
  `,
  styles: `
    .module-page__hero,
    .module-page__footer {
      display: grid;
      gap: 1.2rem;
    }

    .module-page__hero {
      grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
    }

    .module-page__copy {
      display: grid;
      gap: 0.8rem;
      align-content: start;
    }

    .module-page__copy h3 {
      font-size: clamp(1.8rem, 2.4vw, 2.5rem);
      letter-spacing: -0.05em;
    }

    .module-page__copy p {
      color: var(--text-muted);
      line-height: 1.7;
      max-width: 60ch;
    }

    @media (max-width: 960px) {
      .module-page__hero {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ModulePage {
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly summary = input.required<string>();
  readonly bullets = input<string[]>([]);
  readonly metrics = input<ModuleMetric[]>([]);
  readonly actions = input<ModuleAction[]>([]);
  readonly sections = input<ModuleSection[]>([]);
  readonly footerLabel = input('Que puedes hacer aqui');
}
