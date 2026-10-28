import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-rating',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="stars" role="img" [attr.aria-label]="'Note : ' + (value() ?? 0) + ' sur 5'">
      @for (s of stars(); track $index) {
        <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
          [class.on]="s >= 1"
          [class.half]="s > 0 && s < 1"
        >
          <path d="m12 3.6 2.5 5.2 5.7.8-4.1 4 1 5.6L12 16.5l-5.1 2.7 1-5.6-4.1-4 5.7-.8L12 3.6Z" />
        </svg>
      }
    </span>
  `,
  styles: `
    .stars {
      display: inline-flex;
      gap: 2px;
      vertical-align: middle;
    }
    svg {
      width: 16px;
      height: 16px;
      fill: var(--blush-deep);
    }
    svg.on {
      fill: var(--rose);
    }
    svg.half {
      fill: var(--rose);
      opacity: 0.55;
    }
  `,
})
export class RatingComponent {
  readonly value = input<number | null | undefined>(0);
  protected readonly stars = computed(() => {
    const v = this.value() ?? 0;
    return [1, 2, 3, 4, 5].map((i) => Math.max(0, Math.min(1, v - (i - 1))));
  });
}
