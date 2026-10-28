import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
  action?: { label: string; link: string };
}

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  danger?: boolean;
  resolve: (ok: boolean) => void;
}

/** Toast notifications and confirmation dialogs, rendered once in the root layout. */
@Injectable({ providedIn: 'root' })
export class UiService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);
  readonly confirmation = signal<ConfirmRequest | null>(null);

  toast(message: string, kind: ToastKind = 'success', action?: Toast['action']): void {
    const toast: Toast = { id: ++this.seq, kind, message, action };
    this.toasts.update((list) => [...list.slice(-2), toast]);
    setTimeout(() => this.dismiss(toast.id), kind === 'error' ? 7000 : 4500);
  }

  error(message: string): void {
    this.toast(message, 'error');
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  confirm(options: Omit<ConfirmRequest, 'resolve'>): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmation.set({
        ...options,
        resolve: (ok) => {
          this.confirmation.set(null);
          resolve(ok);
        },
      });
    });
  }
}
