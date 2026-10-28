import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { OrderSummary, Page } from '../../models/api.models';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { ImgSizePipe, StatusLabelPipe } from '../../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { PaginationComponent } from '../../shared/components/pagination.component';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-orders',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    MadPipe,
    StatusLabelPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    PaginationComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  template: `
    <h2>Mes commandes</h2>
    @if (failed()) {
      <app-error-state title="Vos commandes n'ont pas pu être chargées" (retry)="load(0)" />
    } @else if (page(); as p) {
      @if (p.content.length === 0) {
        <app-empty-state
          icon="box"
          title="Aucune commande pour l'instant"
          message="Vos commandes apparaîtront ici, avec leur suivi de livraison."
        >
          <a class="btn" routerLink="/boutique">Découvrir la boutique</a>
        </app-empty-state>
      } @else {
        <ul class="orders">
          @for (o of p.content; track o.id) {
            <li>
              <a class="order" [routerLink]="['/compte/commandes', o.id]">
                <img
                  appImgFallback
                  [src]="o.previewImageUrl | imgSize: 160"
                  alt=""
                  width="64"
                  height="80"
                />
                <span class="main">
                  <strong class="ref">{{ o.reference }}</strong>
                  <span class="muted"
                    >Passée le {{ o.createdAt | date: 'd MMMM y' }}, {{ o.itemCount }} article{{
                      o.itemCount > 1 ? 's' : ''
                    }}</span
                  >
                  <span class="muted">Livraison le {{ o.deliveryDate | date: 'EEEE d MMMM' }}</span>
                </span>
                <span class="side">
                  <span class="badge" [class]="'badge status-' + o.status">{{
                    o.status | statusLabel
                  }}</span>
                  <span class="price">{{ o.total | mad }}</span>
                </span>
              </a>
            </li>
          }
        </ul>
        <app-pagination [page]="p.page" [totalPages]="p.totalPages" (pageChange)="load($event)" />
      }
    } @else {
      <div class="skeleton" style="height: 240px"></div>
    }
  `,
  styles: `
    h2 {
      font-size: 1.6rem;
    }
    .orders {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 12px;
    }
    .order {
      display: grid;
      grid-template-columns: 56px 1fr auto;
      gap: 16px;
      align-items: center;
      padding: 14px;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      text-decoration: none;
      color: var(--ink);
      &:hover {
        border-color: var(--rose);
      }
      img {
        width: 56px;
        height: 70px;
        border-radius: 999px 999px 6px 6px;
        object-fit: cover;
        background: var(--blush);
      }
    }
    .main {
      display: grid;
      gap: 2px;
      font-size: 0.92rem;
    }
    .ref {
      font-family: var(--font-display);
      font-weight: 500;
      font-size: 1.1rem;
      color: var(--burgundy);
    }
    .side {
      display: grid;
      justify-items: end;
      gap: 6px;
    }
    @media (max-width: 599px) {
      .order {
        grid-template-columns: 56px 1fr;
      }
      .side {
        grid-column: 2;
        justify-items: start;
      }
    }
    .status-DELIVERED {
      background: var(--sage-soft);
      color: var(--sage-ink);
    }
    .status-CANCELLED {
      background: var(--danger-soft);
      color: var(--danger);
    }
    .status-OUT_FOR_DELIVERY,
    .status-PREPARING {
      background: var(--warning-soft);
      color: var(--warning);
    }
  `,
})
export class OrdersComponent {
  private readonly orders = inject(OrderService);
  protected readonly page = signal<Page<OrderSummary> | null>(null);
  protected readonly failed = signal(false);

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.failed.set(false);
    this.orders.myOrders(page, 10).subscribe({
      next: (p) => this.page.set(p),
      error: () => this.failed.set(true),
    });
  }
}
