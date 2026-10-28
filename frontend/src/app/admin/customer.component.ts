import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../core/services/admin.service';
import { CustomerDetail } from '../models/api.models';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { StatusLabelPipe } from '../shared/pipes/labels.pipes';
import { EmptyStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-customer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, MadPipe, StatusLabelPipe, EmptyStateComponent],
  template: `
    <a routerLink="/admin/clients" class="back">Tous les clients</a>
    @if (data(); as d) {
      <div class="head">
        <div>
          <h1>{{ d.customer.firstName }} {{ d.customer.lastName }}</h1>
          <p>
            <a [href]="'mailto:' + d.customer.email">{{ d.customer.email }}</a>
            @if (d.customer.phone) {
              , {{ d.customer.phone }}
            }
            , client depuis le {{ d.customer.createdAt | date: 'd MMMM y' }}
          </p>
        </div>
        <div class="stats">
          <span
            ><strong>{{ d.customer.orderCount }}</strong> commandes</span
          >
          <span
            ><strong>{{ d.customer.totalSpent | mad }}</strong> dépensés (hors annulations)</span
          >
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Référence</th>
              <th>Date</th>
              <th>Statut</th>
              <th class="num">Total</th>
            </tr>
          </thead>
          <tbody>
            @for (o of d.orders; track o.id) {
              <tr>
                <td>
                  <a [routerLink]="['/admin/commandes', o.id]">{{ o.reference }}</a>
                </td>
                <td>{{ o.createdAt | date: 'd MMM y, HH:mm' }}</td>
                <td>
                  <span [class]="'badge status-' + o.status">{{ o.status | statusLabel }}</span>
                </td>
                <td class="num">{{ o.total | mad }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="4" class="empty-row">Aucune commande.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (missing()) {
      <app-empty-state title="Client introuvable" message="Ce compte client n'existe pas." />
    } @else {
      <div class="skeleton" style="height: 300px"></div>
    }
  `,
  styleUrls: ['./admin-shared.scss'],
  styles: `
    .back {
      display: inline-block;
      margin-block-end: 12px;
      font-size: 0.9rem;
    }
    .stats {
      display: flex;
      gap: 20px;
      flex-wrap: wrap;
      font-size: 0.92rem;
      color: var(--ink-soft);
      strong {
        color: var(--burgundy);
        font-size: 1.2rem;
        font-family: var(--font-display);
        font-weight: 500;
      }
    }
  `,
})
export class CustomerComponent {
  readonly id = input.required<string>();
  private readonly admin = inject(AdminService);
  protected readonly data = signal<CustomerDetail | null>(null);
  protected readonly missing = signal(false);

  constructor() {
    effect(() => {
      this.admin
        .customer(Number(this.id()))
        .subscribe({ next: (d) => this.data.set(d), error: () => this.missing.set(true) });
    });
  }
}
