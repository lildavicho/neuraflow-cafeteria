import { Component, input } from '@angular/core';

@Component({
  selector: 'app-module-pill',
  template: `<span class="module-pill">{{ label() }}</span>`,
  styles: `
    .module-pill {
      display: inline-flex;
      align-items: center;
      border-radius: 999px;
      border: 1px solid rgba(148, 163, 184, 0.16);
      padding: 0.42rem 0.78rem;
      background: rgba(255, 255, 255, 0.08);
      color: rgba(226, 232, 240, 0.92);
      font-size: 0.75rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      font-weight: 700;
    }
  `,
})
export class ModulePill {
  readonly label = input.required<string>();
}
