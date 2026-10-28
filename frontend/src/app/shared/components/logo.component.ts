import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Bloomé wordmark: Fraunces (soft axis, display optical size) followed by a single rose petal. */
@Component({
  selector: 'app-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="wordmark"
      role="img"
      aria-label="Bloomé"
      [class.light]="light()"
      [style.font-size.px]="size()"
    >
      <span aria-hidden="true">Bloomé</span
      ><svg class="petal" viewBox="0 0 20 20" aria-hidden="true">
        <path d="M5 16C5 9 9.5 4 16 3.5 16 10 11.5 15.5 5 16Z" />
        <path d="M5 16 12 9" class="vein" />
      </svg>
    </span>
  `,
  styles: `
    .wordmark {
      font-family: var(--font-display);
      font-weight: 500;
      font-variation-settings:
        'SOFT' 100,
        'opsz' 144,
        'WONK' 1;
      letter-spacing: -0.02em;
      color: var(--burgundy);
      line-height: 1;
      display: inline-block;
      white-space: nowrap;
    }
    .light {
      color: var(--cream);
    }
    .petal {
      display: inline-block;
      width: 0.42em;
      height: 0.42em;
      margin-inline-start: 0.04em;
      vertical-align: 0.55em;
      fill: var(--rose);
    }
    .vein {
      fill: none;
      stroke: var(--cream);
      stroke-width: 1.4;
      stroke-linecap: round;
    }
    .light .petal {
      fill: var(--blush-deep);
    }
    .light .vein {
      stroke: var(--burgundy);
    }
  `,
})
export class LogoComponent {
  readonly size = input(30);
  readonly light = input(false);
}
