import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { IconComponent } from './icon.component';

/** Zero-based pagination with a compact window of page numbers. */
@Component({
  selector: 'app-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    @if (totalPages() > 1) {
      <nav class="pager" aria-label="Pagination">
        <button
          type="button"
          class="icon-btn"
          [disabled]="page() === 0"
          (click)="go(page() - 1)"
          aria-label="Page précédente"
        >
          <app-icon name="chevronLeft" />
        </button>
        @for (p of pages(); track $index) {
          @if (p === -1) {
            <span class="gap" aria-hidden="true">…</span>
          } @else {
            <button
              type="button"
              class="num"
              [class.current]="p === page()"
              [attr.aria-current]="p === page() ? 'page' : null"
              (click)="go(p)"
            >
              {{ p + 1 }}
            </button>
          }
        }
        <button
          type="button"
          class="icon-btn"
          [disabled]="page() >= totalPages() - 1"
          (click)="go(page() + 1)"
          aria-label="Page suivante"
        >
          <app-icon name="chevronRight" />
        </button>
      </nav>
    }
  `,
  styles: `
    .pager {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      margin-block-start: 40px;
      flex-wrap: wrap;
    }
    .num {
      min-width: 44px;
      height: 44px;
      border-radius: 50%;
      border: 1px solid transparent;
      background: transparent;
      color: var(--ink);
      font: 600 0.95rem var(--font-body);
      cursor: pointer;
      &:hover {
        background: var(--blush);
      }
      &.current {
        background: var(--burgundy);
        color: var(--white);
      }
    }
    .gap {
      padding-inline: 4px;
      color: var(--ink-soft);
    }
    .icon-btn:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }
  `,
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();

  protected readonly pages = computed(() => {
    const total = this.totalPages();
    const current = this.page();
    const set = new Set<number>([0, total - 1, current - 1, current, current + 1]);
    const sorted = [...set].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
    const result: number[] = [];
    sorted.forEach((p, i) => {
      if (i > 0 && p - sorted[i - 1] > 1) result.push(-1);
      result.push(p);
    });
    return result;
  });

  go(page: number): void {
    if (page >= 0 && page < this.totalPages() && page !== this.page()) {
      this.pageChange.emit(page);
    }
  }
}
