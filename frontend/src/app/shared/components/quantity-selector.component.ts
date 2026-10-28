import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { IconComponent } from './icon.component';

/** Accessible stepper constrained to [min, max]. */
@Component({
  selector: 'app-quantity-selector',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="qty" role="group" [attr.aria-label]="label()">
      <button
        type="button"
        (click)="set(value() - 1)"
        [disabled]="disabled() || value() <= min()"
        aria-label="Diminuer la quantité"
      >
        <app-icon name="minus" [size]="18" />
      </button>
      <input
        type="number"
        inputmode="numeric"
        [attr.aria-label]="label()"
        [min]="min()"
        [max]="max()"
        [value]="value()"
        [disabled]="disabled()"
        (change)="onInput($event)"
      />
      <button
        type="button"
        (click)="set(value() + 1)"
        [disabled]="disabled() || value() >= max()"
        aria-label="Augmenter la quantité"
      >
        <app-icon name="plus" [size]="18" />
      </button>
    </div>
  `,
  styles: `
    .qty {
      display: inline-flex;
      align-items: center;
      border: 1.5px solid var(--line);
      border-radius: 999px;
      background: var(--white);
      height: 46px;
    }
    button {
      display: grid;
      place-items: center;
      width: 42px;
      height: 100%;
      border: 0;
      background: transparent;
      color: var(--burgundy);
      cursor: pointer;
      border-radius: 999px;
      &:hover:not(:disabled) {
        background: var(--blush);
      }
      &:disabled {
        opacity: 0.35;
        cursor: not-allowed;
      }
    }
    input {
      width: 42px;
      border: 0;
      text-align: center;
      font: 600 1rem var(--font-body);
      color: var(--ink);
      background: transparent;
      -moz-appearance: textfield;
      &::-webkit-outer-spin-button,
      &::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }
  `,
})
export class QuantitySelectorComponent {
  readonly value = model(1);
  readonly min = input(1);
  readonly max = input(10);
  readonly disabled = input(false);
  readonly label = input('Quantité');

  set(next: number): void {
    const clamped = Math.max(this.min(), Math.min(this.max(), Math.round(next)));
    if (!Number.isNaN(clamped) && clamped !== this.value()) {
      this.value.set(clamped);
    }
  }

  onInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    this.set(Number(el.value));
    el.value = String(this.value());
  }
}
