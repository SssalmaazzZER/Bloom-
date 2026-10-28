import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../core/services/admin.service';
import { CustomerSummary, Page } from '../models/api.models';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { PaginationComponent } from '../shared/components/pagination.component';
import { ErrorStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-customers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, MadPipe, PaginationComponent, ErrorStateComponent],
  template: `
    <div class="head">
      <div>
        <h1>Clients</h1>
        <p>{{ page()?.totalElements ?? '…' }} compte(s) client.</p>
      </div>
    </div>
    <form class="toolbar" role="search" (submit)="search($event)">
      <label
        >Recherche <input class="input" name="q" type="search" placeholder="Nom ou e-mail"
      /></label>
      <button type="submit" class="btn btn-sm">Rechercher</button>
    </form>
    @if (failed()) {
      <app-error-state (retry)="load()" />
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Client</th>
              <th>E-mail</th>
              <th>Téléphone</th>
              <th>Inscrit le</th>
              <th class="num">Commandes</th>
              <th class="num">Total dépensé</th>
            </tr>
          </thead>
          <tbody>
            @for (c of page()?.content ?? []; track c.id) {
              <tr>
                <td>
                  <a [routerLink]="['/admin/clients', c.id]">{{ c.firstName }} {{ c.lastName }}</a>
                </td>
                <td>{{ c.email }}</td>
                <td>{{ c.phone ?? '—' }}</td>
                <td>{{ c.createdAt | date: 'd MMM y' }}</td>
                <td class="num">{{ c.orderCount }}</td>
                <td class="num">{{ c.totalSpent | mad }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="6" class="empty-row">
                  {{ page() ? 'Aucun client trouvé.' : 'Chargement…' }}
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
export class CustomersComponent {
  private readonly admin = inject(AdminService);
  protected readonly page = signal<Page<CustomerSummary> | null>(null);
  protected readonly failed = signal(false);
  private q = '';
  private current = 0;

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.admin
      .customers({ q: this.q, page: this.current, size: 20 })
      .subscribe({ next: (p) => this.page.set(p), error: () => this.failed.set(true) });
  }

  search(event: Event): void {
    event.preventDefault();
    this.q = (
      (event.target as HTMLFormElement).elements.namedItem('q') as HTMLInputElement
    ).value.trim();
    this.current = 0;
    this.load();
  }

  goTo(p: number): void {
    this.current = p;
    this.load();
  }
}
