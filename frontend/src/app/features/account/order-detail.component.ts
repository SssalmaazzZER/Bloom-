import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { OrderDetail } from '../../models/api.models';
import { OrderViewComponent } from '../../shared/components/order-view.component';
import { EmptyStateComponent } from '../../shared/components/states.component';
import { StatusLabelPipe } from '../../shared/pipes/labels.pipes';

@Component({
  selector: 'app-order-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, OrderViewComponent, EmptyStateComponent, StatusLabelPipe],
  template: `
    <a routerLink="/compte/commandes" class="back">Toutes mes commandes</a>
    @if (order(); as o) {
      <header class="head">
        <div>
          <h2>Commande {{ o.reference }}</h2>
          <p class="muted">
            Passée le {{ o.createdAt | date: "d MMMM y 'à' HH:mm" }} · Statut :
            <strong>{{ o.status | statusLabel }}</strong>
          </p>
        </div>
        @if (o.cancellable) {
          <button
            type="button"
            class="btn btn-ghost btn-sm"
            [disabled]="cancelling()"
            (click)="cancel(o)"
          >
            Annuler la commande
          </button>
        }
      </header>
      <app-order-view [order]="o" />
      @if (!o.cancellable && o.status !== 'CANCELLED' && o.status !== 'DELIVERED') {
        <p class="muted small">
          Votre commande est en préparation : pour la modifier, appelez-nous au
          <a href="tel:+212537000000">05 37 00 00 00</a>.
        </p>
      }
    } @else if (missing()) {
      <app-empty-state
        title="Commande introuvable"
        message="Cette commande n'existe pas ou n'est pas associée à votre compte."
      >
        <a class="btn" routerLink="/compte/commandes">Mes commandes</a>
      </app-empty-state>
    } @else {
      <div class="skeleton" style="height: 360px"></div>
    }
  `,
  styles: `
    .back {
      display: inline-block;
      margin-block-end: 14px;
      font-size: 0.92rem;
    }
    .head {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: start;
      gap: 16px;
      margin-block-end: 24px;
      h2 {
        font-size: 1.6rem;
        margin-block-end: 4px;
      }
    }
    .small {
      font-size: 0.9rem;
      margin-block-start: 16px;
    }
  `,
})
export class OrderDetailComponent {
  readonly id = input.required<string>();
  private readonly orders = inject(OrderService);
  private readonly ui = inject(UiService);
  protected readonly order = signal<OrderDetail | null>(null);
  protected readonly missing = signal(false);
  protected readonly cancelling = signal(false);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.missing.set(false);
      this.order.set(null);
      if (!Number.isInteger(id) || id <= 0) {
        this.missing.set(true);
        return;
      }
      this.orders
        .get(id)
        .subscribe({ next: (o) => this.order.set(o), error: () => this.missing.set(true) });
    });
  }

  async cancel(order: OrderDetail): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Annuler cette commande ?',
      message: `La commande ${order.reference} sera annulée et les fleurs remises en vente. Cette action est définitive.`,
      confirmLabel: 'Annuler la commande',
      cancelLabel: 'Garder ma commande',
      danger: true,
    });
    if (!ok) return;
    this.cancelling.set(true);
    this.orders.cancel(order.id).subscribe({
      next: (o) => {
        this.cancelling.set(false);
        this.order.set(o);
        this.ui.toast(`Commande ${o.reference} annulée.`, 'info');
      },
      error: (err) => {
        this.cancelling.set(false);
        this.ui.error(errorMessage(err));
      },
    });
  }
}
