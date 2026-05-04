import { Component, HostBinding, input } from '@angular/core';

type BrandSize = 'small' | 'medium' | 'large' | 'hero';

@Component({
  selector: 'app-brand',
  template: `
    <span class="iv-brand" [attr.data-size]="size()" [class.iv-brand--mono]="monochrome()">
      @if (iconOnly()) {
        <img class="iv-brand__icon" src="/favicon.svg" alt="InsightVision" width="80" height="80" />
      } @else {
        <img
          class="iv-brand__logo iv-brand__logo--light"
          src="/brand/insightvision-logo-light.svg"
          alt="InsightVision Enterprise AI"
        />
        <img
          class="iv-brand__logo iv-brand__logo--dark"
          src="/brand/insightvision-logo-dark.svg"
          alt="InsightVision Enterprise AI"
        />
      }
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
      min-width: 0;
      color: var(--text-strong);
    }

    .iv-brand {
      --brand-logo-width: 168px;
      --brand-icon-size: 38px;
      display: inline-flex;
      align-items: center;
      min-width: 0;
      color: inherit;
      line-height: 0;
    }

    .iv-brand[data-size='small'] {
      --brand-logo-width: 148px;
      --brand-icon-size: 34px;
    }

    .iv-brand[data-size='medium'] {
      --brand-logo-width: 206px;
      --brand-icon-size: 44px;
    }

    .iv-brand[data-size='large'] {
      --brand-logo-width: 276px;
      --brand-icon-size: 62px;
    }

    .iv-brand[data-size='hero'] {
      --brand-logo-width: min(360px, 78vw);
      --brand-icon-size: 78px;
    }

    .iv-brand__logo {
      width: var(--brand-logo-width);
      max-width: 100%;
      height: auto;
      display: block;
    }

    .iv-brand__logo--dark {
      display: none;
    }

    :host-context([data-theme='dark']) .iv-brand__logo--light {
      display: none;
    }

    :host-context([data-theme='dark']) .iv-brand__logo--dark {
      display: block;
    }

    .iv-brand__icon {
      width: var(--brand-icon-size);
      height: var(--brand-icon-size);
      display: block;
      object-fit: contain;
    }

    .iv-brand--mono {
      filter: grayscale(1) contrast(1.1);
      opacity: 0.92;
    }

    @media (max-width: 420px) {
      .iv-brand[data-size='small'] {
        --brand-logo-width: 132px;
      }
    }
  `,
})
export class Brand {
  readonly size = input<BrandSize>('medium');
  readonly iconOnly = input(false);
  readonly monochrome = input(false);
  readonly className = input('');

  @HostBinding('class')
  protected get hostClasses(): string {
    return this.className();
  }
}
