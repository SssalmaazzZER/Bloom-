import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminService } from '../core/services/admin.service';
import { OrderStatus, OrderSummary, Page } from '../models/api.models';
import { STATUS_LABELS } from '../models/labels';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { StatusLabelPipe } from '../shared/pipes/labels.pipes';
import { PaginationComponent } from '../shared/components/pagination.component';
import { ErrorStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-admin-orders',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    MadPipe,
    StatusLabelPipe,
    PaginationComponent,
    ErrorStateComponent,
  ],
  template: `
    <div class="head">
      <div>
        <h1>Commandes</h1>
        <p>{{ page()?.totalElements ?? '…' }} commande(s) correspondant aux filtres.</p>
      </div>
    </div>
    <form class="toolbar" (submit)="search($event)" role="search">
      <label
        >Recherche
        <input
          class="input"
          name="q"
          type="search"
          [value]="q()"
          placeholder="Référence, client, e-mail…"
        />
      </label>
      <label
        >Statut
        <select class="select" name="status" (change)="setStatus($event)">
          <option value="">Tous</option>
          @for (s of statuses; track s) {
            <option [value]="s" [selected]="status() === s">{{ s | statusLabel }}</option>
          }
        </select>
      </label>
      <label>Du <input class="input" type="date" name="from" [value]="from()" /></label>
      <label>Au <input class="input" type="date" name="to" [value]="to()" /></label>
      <button type="submit" class="btn btn-sm">Filtrer</button>
      <button type="button" class="btn btn-ghost btn-sm" (click)="reset()">Réinitialiser</button>
    </form>

    @if (failed()) {
      <app-error-state (retry)="load()" />
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Référence</th>
              <th>Passée le</th>
              <th>Client</th>
              <th>Livraison</th>
              <th>Statut</th>
              <th class="num">Articles</th>
              <th class="num">Total</th>
            </tr>
          </thead>
          <tbody>
            @for (o of page()?.content ?? []; track o.id) {
              <tr>
                <td>
                  <a [routerLink]="['/admin/commandes', o.id]">{{ o.reference }}</a>
                </td>
                <td>{{ o.createdAt | date: 'd MMM y, HH:mm' }}</td>
                <td>{{ o.customerName }}</td>
                <td>
                  {{ o.deliveryDate | date: 'EEE d MMM' }}<br /><span class="muted">{{
                    o.city
                  }}</span>
                </td>
                <td>
                  <span [class]="'badge status-' + o.status">{{ o.status | statusLabel }}</span>
                </td>
                <td class="num">{{ o.itemCount }}</td>
                <td class="num">{{ o.total | mad }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="empty-row">
                  {{ page() ? 'Aucune commande ne correspond à ces filtres.' : 'Chargement…' }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      @if (page(); as p) {
        <app-pagination [page]="p.page" [totalPages]="p.totalPages" (pageChange)="goTo($event)" />
      }
    }
  `,
  styleUrls: ['./admin-shared.scss'],
})
export class AdminOrdersComponent {
  private readonly admin = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly statuses = Object.keys(STATUS_LABELS) as OrderStatus[];
  protected readonly page = signal<Page<OrderSummary> | null>(null);
  protected readonly failed = signal(false);
  protected readonly q = signal('');
  protected readonly status = signal<OrderStatus | ''>('');
  protected readonly from = signal('');
  protected readonly to = signal('');
  private current = 0;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((p) => {
      this.q.set(p.get('q') ?? '');
      this.status.set((p.get('status') as OrderStatus) ?? '');
      this.from.set(p.get('from') ?? '');
      this.to.set(p.get('to') ?? '');
      this.current = Math.max(0, Number(p.get('page') ?? 0) || 0);
      this.load();
    });
  }

  load(): void {
    this.failed.set(false);
    this.admin
      .orders({
        q: this.q(),
        status: this.status(),
        from: this.from(),
        to: this.to(),
        page: this.current,
        size: 20,
      })
      .subscribe({ next: (p) => this.page.set(p), error: () => this.failed.set(true) });
  }

  search(event: Event): void {
    event.preventDefault();
    const f = event.target as HTMLFormElement;
    const value = (name: string) => (f.elements.namedItem(name) as HTMLInputElement).value || null;
    this.navigate({
      q: value('q'),
      status: value('status'),
      from: value('from'),
      to: value('to'),
      page: null,
    });
  }

  setStatus(event: Event): void {
    this.navigate({ status: (event.target as HTMLSelectElement).value || null, page: null });
  }

  reset(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  goTo(page: number): void {
    this.navigate({ page: page || null });
  }

  private navigate(params: Record<string, string | number | null>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }
}
