import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  inject,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiService } from '../../core/services/ui.service';
import { IconComponent } from './icon.component';

/** Toast stack + confirmation dialog, mounted once in the root component. */
@Component({
  selector: 'app-overlays',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent],
  template: `
    <div class="toasts" aria-live="polite" aria-atomic="false">
      @for (t of ui.toasts(); track t.id) {
        <div
          class="toast"
          [class]="'toast ' + t.kind"
          [attr.role]="t.kind === 'error' ? 'alert' : 'status'"
        >
          <app-icon
            [name]="t.kind === 'error' ? 'alert' : t.kind === 'success' ? 'check' : 'leaf'"
            [size]="20"
          />
          <span class="msg">{{ t.message }}</span>
          @if (t.action) {
            <a [routerLink]="t.action.link" (click)="ui.dismiss(t.id)">{{ t.action.label }}</a>
          }
          <button
            type="button"
            class="x"
            (click)="ui.dismiss(t.id)"
            aria-label="Fermer la notification"
          >
            <app-icon name="close" [size]="16" />
          </button>
        </div>
      }
    </div>

    <dialog
      #dialog
      class="confirm"
      (cancel)="answer(false, $event)"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-msg"
    >
      @if (ui.confirmation(); as c) {
        <h2 id="confirm-title">{{ c.title }}</h2>
        <p id="confirm-msg">{{ c.message }}</p>
        <div class="row">
          <button type="button" class="btn btn-ghost" (click)="answer(false)">
            {{ c.cancelLabel ?? 'Annuler' }}
          </button>
          <button type="button" class="btn" [class.btn-danger]="c.danger" (click)="answer(true)">
            {{ c.confirmLabel }}
          </button>
        </div>
      }
    </dialog>
  `,
  styles: `
    .toasts {
      position: fixed;
      z-index: 90;
      inset-block-end: 20px;
      inset-inline-end: 20px;
      display: grid;
      gap: 10px;
      width: min(420px, calc(100vw - 40px));
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 14px 14px 18px;
      background: var(--burgundy);
      color: var(--cream);
      border-radius: var(--radius);
      box-shadow: var(--shadow-pop);
      font-size: 0.95rem;
      animation: rise 0.35s var(--ease);
      a {
        color: var(--blush);
        font-weight: 600;
        white-space: nowrap;
      }
      &.error {
        background: var(--danger);
      }
      &.info {
        background: var(--ink);
      }
    }
    .msg {
      flex: 1;
    }
    .x {
      border: 0;
      background: transparent;
      color: inherit;
      cursor: pointer;
      padding: 6px;
      border-radius: 50%;
      opacity: 0.8;
      &:hover {
        opacity: 1;
      }
    }
    @keyframes rise {
      from {
        transform: translateY(12px);
        opacity: 0;
      }
    }
    .confirm {
      border: 0;
      border-radius: var(--radius-lg);
      padding: 32px;
      width: min(460px, calc(100vw - 32px));
      background: var(--cream);
      color: var(--ink);
      box-shadow: var(--shadow-pop);
      &::backdrop {
        background: rgba(48, 42, 41, 0.45);
      }
      h2 {
        font-size: 1.6rem;
      }
      p {
        color: var(--ink-soft);
      }
    }
    .row {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      flex-wrap: wrap;
      margin-block-start: 24px;
    }
  `,
})
export class OverlaysComponent {
  protected readonly ui = inject(UiService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const open = this.ui.confirmation() !== null;
      const el = this.dialog().nativeElement;
      if (open && !el.open) el.showModal();
      if (!open && el.open) el.close();
    });
  }

  answer(ok: boolean, event?: Event): void {
    event?.preventDefault();
    this.ui.confirmation()?.resolve(ok);
  }
}
