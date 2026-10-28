import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../core/services/admin.service';
import { Dashboard, OrderStatus } from '../models/api.models';
import { STATUS_LABELS } from '../models/labels';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { StatusLabelPipe } from '../shared/pipes/labels.pipes';
import { ErrorStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, MadPipe, StatusLabelPipe, ErrorStateComponent],
  template: `
    <div class="head">
      <div>
        <h1>Tableau de bord</h1>
        <p>Données en direct de la base, actualisées à {{ loadedAt() | date: 'HH:mm' }}.</p>
      </div>
      <button type="button" class="btn btn-soft btn-sm" (click)="load()">Actualiser</button>
    </div>

    @if (failed()) {
      <app-error-state title="Le tableau de bord n'a pas pu être chargé" (retry)="load()" />
    } @else if (data(); as d) {
      <section class="kpis" aria-label="Indicateurs clés">
        <div class="kpi">
          <span class="label">Chiffre d'affaires</span><strong>{{ d.revenue | mad }}</strong
          ><span class="sub">dont {{ d.deliveredRevenue | mad }} encaissés</span>
        </div>
        <a class="kpi" routerLink="/admin/commandes"
          ><span class="label">Commandes</span><strong>{{ d.totalOrders }}</strong
          ><span class="sub">toutes périodes</span></a
        >
        <a class="kpi accent" routerLink="/admin/commandes" [queryParams]="{ status: 'PENDING' }"
          ><span class="label">À confirmer</span><strong>{{ d.pendingOrders }}</strong
          ><span class="sub">commandes reçues</span></a
        >
        <a class="kpi" routerLink="/admin/clients"
          ><span class="label">Clients</span><strong>{{ d.totalCustomers }}</strong
          ><span class="sub">comptes clients</span></a
        >
        <a class="kpi" routerLink="/admin/produits"
          ><span class="label">Produits actifs</span><strong>{{ d.activeProducts }}</strong
          ><span class="sub">en boutique</span></a
        >
        <a class="kpi" [class.warn]="d.lowStockCount > 0" routerLink="/admin/inventaire"
          ><span class="label">Stock bas</span><strong>{{ d.lowStockCount }}</strong
          ><span class="sub">produits et fleurs</span></a
        >
      </section>
      <p class="definition">{{ d.revenueDefinition }}</p>

      <section class="card status" aria-labelledby="status-title">
        <h2 id="status-title">Commandes par statut</h2>
        <ul>
          @for (s of statuses; track s) {
            <li>
              <a routerLink="/admin/commandes" [queryParams]="{ status: s }">
                <span class="name">{{ s | statusLabel }}</span>
                <span class="bar"
                  ><span [class]="'fill status-' + s" [style.width.%]="share(d, s)"></span
                ></span>
                <span class="count">{{ d.ordersByStatus[s] ?? 0 }}</span>
              </a>
            </li>
          }
        </ul>
      </section>

      <div class="two">
        <section class="card" aria-labelledby="recent-title">
          <div class="card-head">
            <h2 id="recent-title">Dernières commandes</h2>
            <a routerLink="/admin/commandes">Tout voir</a>
          </div>
          <div class="table-wrap flat">
            <table>
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Client</th>
                  <th>Statut</th>
                  <th class="num">Total</th>
                </tr>
              </thead>
              <tbody>
                @for (o of d.recentOrders; track o.id) {
                  <tr>
                    <td>
                      <a [routerLink]="['/admin/commandes', o.id]">{{ o.reference }}</a
                      ><br /><span class="muted small">{{
                        o.createdAt | date: 'd MMM, HH:mm'
                      }}</span>
                    </td>
                    <td>
                      {{ o.customerName }}<br /><span class="muted small">{{ o.city }}</span>
                    </td>
                    <td>
                      <span [class]="'badge status-' + o.status">{{ o.status | statusLabel }}</span>
                    </td>
                    <td class="num">{{ o.total | mad }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" class="empty-row">Aucune commande pour le moment.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <div class="stack">
          <section class="card" aria-labelledby="best-title">
            <h2 id="best-title">Meilleures ventes</h2>
            <ol class="best">
              @for (b of d.bestsellers; track b.productId) {
                <li>
                  <span>{{ b.name }}</span
                  ><span class="muted">{{ b.quantitySold }} vendus · {{ b.revenue | mad }}</span>
                </li>
              } @empty {
                <li class="muted">Les ventes apparaîtront ici après les premières commandes.</li>
              }
            </ol>
          </section>
          <section class="card" aria-labelledby="low-title">
            <div class="card-head">
              <h2 id="low-title">Alertes de stock</h2>
              <a routerLink="/admin/inventaire">Inventaire</a>
            </div>
            <ul class="low">
              @for (l of d.lowStock; track l.kind + l.id) {
                <li>
                  <span
                    >{{ l.name }}
                    <span class="muted small">{{
                      l.kind === 'STEM' ? '(fleur à l’unité)' : ''
                    }}</span></span
                  >
                  <span
                    class="badge"
                    [class.danger]="l.stockQuantity === 0"
                    [class.warn]="l.stockQuantity > 0"
                  >
                    {{ l.stockQuantity === 0 ? 'Épuisé' : l.stockQuantity + ' en stock' }}
                  </span>
                </li>
              } @empty {
                <li class="muted">Aucun stock bas.</li>
              }
            </ul>
          </section>
        </div>
      </div>
    } @else {
      <div class="skeleton" style="height: 420px"></div>
    }
  `,
  styleUrls: ['./admin-shared.scss'],
  styles: `
    .kpis {
      display: grid;
      gap: 14px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 180px), 1fr));
    }
    .kpi {
      display: grid;
      gap: 2px;
      padding: 18px;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      text-decoration: none;
      color: var(--ink);
      .label {
        font-size: 0.85rem;
        color: var(--ink-soft);
        font-weight: 600;
      }
      strong {
        font-family: var(--font-display);
        font-weight: 500;
        font-size: 1.9rem;
        color: var(--burgundy);
        line-height: 1.2;
        font-variant-numeric: lining-nums;
      }
      .sub {
        font-size: 0.8rem;
        color: var(--ink-soft);
      }
    }
    a.kpi:hover {
      border-color: var(--rose);
    }
    .kpi.accent {
      background: var(--blush);
      border-color: var(--blush-deep);
    }
    .kpi.warn strong {
      color: var(--warning);
    }
    .definition {
      font-size: 0.82rem;
      color: var(--ink-soft);
      margin: 10px 0 22px;
      max-width: none;
    }
    .card h2 {
      font-size: 1.25rem;
      margin: 0 0 14px;
    }
    .card-head {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 10px;
      a {
        font-size: 0.88rem;
      }
    }
    .status ul {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 8px;
    }
    .status a {
      display: grid;
      grid-template-columns: 130px 1fr 40px;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: var(--ink);
      font-size: 0.92rem;
    }
    .bar {
      height: 10px;
      border-radius: 999px;
      background: #f5ece9;
      overflow: hidden;
    }
    .fill {
      display: block;
      height: 100%;
      border-radius: 999px;
      min-width: 2px;
      background: var(--rose);
    }
    .fill.status-CONFIRMED {
      background: #5a77a8;
    }
    .fill.status-PREPARING,
    .fill.status-OUT_FOR_DELIVERY {
      background: #c9913d;
    }
    .fill.status-DELIVERED {
      background: var(--sage);
    }
    .fill.status-CANCELLED {
      background: var(--danger);
    }
    .count {
      text-align: end;
      font-variant-numeric: tabular-nums;
      font-weight: 600;
    }
    .two {
      display: grid;
      gap: 20px;
      margin-block-start: 20px;
    }
    @media (min-width: 1100px) {
      .two {
        grid-template-columns: 1.5fr 1fr;
      }
    }
    .stack {
      display: grid;
      gap: 20px;
      align-content: start;
    }
    .flat {
      border: 0;
    }
    .small {
      font-size: 0.8rem;
    }
    .best,
    .low {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 10px;
      li {
        display: flex;
        justify-content: space-between;
        gap: 10px;
        font-size: 0.92rem;
        align-items: center;
      }
    }
  `,
})
export class DashboardComponent {
  private readonly admin = inject(AdminService);
  protected readonly data = signal<Dashboard | null>(null);
  protected readonly failed = signal(false);
  protected readonly loadedAt = signal(new Date());
  protected readonly statuses = Object.keys(STATUS_LABELS) as OrderStatus[];
  private readonly max = computed(() =>
    Math.max(1, ...Object.values(this.data()?.ordersByStatus ?? {})),
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.admin.dashboard().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loadedAt.set(new Date());
      },
      error: () => this.failed.set(true),
    });
  }

  share(d: Dashboard, s: OrderStatus): number {
    return ((d.ordersByStatus[s] ?? 0) / this.max()) * 100;
  }
}
