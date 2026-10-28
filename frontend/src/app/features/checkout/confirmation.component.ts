import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { OrderDetail } from '../../models/api.models';
import { OrderViewComponent } from '../../shared/components/order-view.component';
import { IconComponent } from '../../shared/components/icon.component';
import { EmptyStateComponent } from '../../shared/components/states.component';
import { MadPipe } from '../../shared/pipes/mad.pipe';

@Component({
  selector: 'app-confirmation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    LowerCasePipe,
    OrderViewComponent,
    IconComponent,
    EmptyStateComponent,
    MadPipe,
  ],
  template: `
    <div class="container">
      @if (order(); as o) {
        <header class="thanks">
          <span class="seal" aria-hidden="true"
            ><app-icon name="check" [size]="34" [stroke]="2"
          /></span>
          <h1>Merci, votre commande est reçue</h1>
          <p class="lede">
            Référence <strong class="ref">{{ o.reference }}</strong
            >. Nous préparons vos fleurs pour le {{ o.deliveryDate | date: 'EEEE d MMMM' }} ({{
              o.deliverySlotLabel | lowercase
            }}). Vous réglerez {{ o.total | mad }} au coursier à la livraison.
          </p>
          <div class="actions">
            <a class="btn" [routerLink]="['/compte/commandes', o.id]">Suivre ma commande</a>
            <a class="btn btn-ghost" routerLink="/boutique">Continuer mes achats</a>
          </div>
        </header>
        <app-order-view [order]="o" />
      } @else if (missing()) {
        <div class="section">
          <app-empty-state
            title="Commande introuvable"
            message="Cette commande n'existe pas ou n'est pas associée à votre compte."
          >
            <a class="btn" routerLink="/compte/commandes">Mes commandes</a>
          </app-empty-state>
        </div>
      } @else {
        <div class="skeleton" style="height: 400px; margin-block: 48px"></div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      padding-block-end: 96px;
    }
    .thanks {
      text-align: center;
      padding-block: 56px 40px;
      display: grid;
      justify-items: center;
    }
    .seal {
      display: grid;
      place-items: center;
      width: 76px;
      height: 76px;
      border-radius: 999px 999px 18px 18px;
      background: var(--sage);
      color: var(--white);
      margin-block-end: 20px;
    }
    h1 {
      max-width: 18ch;
    }
    .lede {
      max-width: 56ch;
    }
    .ref {
      font-family: var(--font-display);
      color: var(--burgundy);
      letter-spacing: 0.02em;
    }
    .actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      justify-content: center;
      margin-block-start: 12px;
    }
  `,
})
export class ConfirmationComponent {
  readonly id = input.required<string>();
  private readonly orders = inject(OrderService);
  protected readonly order = signal<OrderDetail | null>(null);
  protected readonly missing = signal(false);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      if (!Number.isInteger(id) || id <= 0) {
        this.missing.set(true);
        return;
      }
      this.orders.get(id).subscribe({
        next: (o) => this.order.set(o),
        error: () => this.missing.set(true),
      });
    });
  }
}
