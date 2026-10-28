import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AdminService } from '../core/services/admin.service';
import { CatalogService } from '../core/services/catalog.service';
import { UiService } from '../core/services/ui.service';
import { errorMessage } from '../core/errors';
import { AdminProduct, Page } from '../models/api.models';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { ImgSizePipe } from '../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../shared/directives/img-fallback.directive';
import { PaginationComponent } from '../shared/components/pagination.component';
import { ErrorStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-admin-products',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MadPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    PaginationComponent,
    ErrorStateComponent,
  ],
  template: `
    <div class="head">
      <div>
        <h1>Produits</h1>
        <p>{{ page()?.totalElements ?? '…' }} produit(s)</p>
      </div>
      <a class="btn btn-sm" routerLink="/admin/produits/nouveau">Nouveau produit</a>
    </div>
    <form class="toolbar" role="search" (submit)="search($event)">
      <label
        >Recherche <input class="input" name="q" type="search" placeholder="Nom, description…"
      /></label>
      <label
        >Catégorie
        <select class="select" name="category">
          <option value="">Toutes</option>
          @for (c of categories(); track c.id) {
            <option [value]="c.slug">{{ c.name }}</option>
          }
        </select>
      </label>
      <label
        >État
        <select class="select" name="active">
          <option value="">Tous</option>
          <option value="true">Actifs</option>
          <option value="false">Désactivés</option>
        </select>
      </label>
      <label class="inline"><input type="checkbox" name="lowStock" /> Stock bas uniquement</label>
      <button type="submit" class="btn btn-sm">Filtrer</button>
    </form>

    @if (failed()) {
      <app-error-state (retry)="load()" />
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th><span class="visually-hidden">Photo</span></th>
              <th>Produit</th>
              <th>Catégorie</th>
              <th class="num">Prix</th>
              <th class="num">Stock</th>
              <th class="num">Ventes</th>
              <th>État</th>
              <th><span class="visually-hidden">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            @for (p of page()?.content ?? []; track p.id) {
              <tr>
                <td>
                  <img
                    class="thumb"
                    appImgFallback
                    [src]="p.images[0]?.url | imgSize: 120"
                    alt=""
                  />
                </td>
                <td>
                  <a [routerLink]="['/admin/produits', p.id]">{{ p.name }}</a
                  ><br /><span class="muted small">{{ p.slug }}</span>
                </td>
                <td>{{ p.categoryName }}</td>
                <td class="num">{{ p.price | mad }}</td>
                <td class="num">
                  <span
                    class="badge"
                    [class.danger]="p.stockQuantity === 0"
                    [class.warn]="p.lowStock && p.stockQuantity > 0"
                    [class.sage]="!p.lowStock"
                    >{{ p.stockQuantity }}</span
                  >
                </td>
                <td class="num">{{ p.salesCount }}</td>
                <td>
                  @if (p.active) {
                    <span class="badge sage">Actif</span>
                  } @else {
                    <span class="badge">Désactivé</span>
                  }
                  @if (p.featured) {
                    <span class="badge ink">Vedette</span>
                  }
                </td>
                <td class="actions">
                  <a class="btn btn-ghost btn-sm" [routerLink]="['/admin/produits', p.id]"
                    >Modifier</a
                  >
                  @if (p.active) {
                    <button type="button" class="link-btn danger" (click)="deactivate(p)">
                      Désactiver
                    </button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="8" class="empty-row">
                  {{ page() ? 'Aucun produit trouvé.' : 'Chargement…' }}
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
  styles: `
    .small {
      font-size: 0.8rem;
    }
    .inline {
      display: flex !important;
      align-items: center;
      gap: 8px;
      min-height: 42px;
    }
    .actions {
      white-space: nowrap;
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .danger {
      color: var(--danger);
    }
  `,
})
export class AdminProductsComponent {
  private readonly admin = inject(AdminService);
  private readonly catalog = inject(CatalogService);
  private readonly ui = inject(UiService);

  protected readonly categories = toSignal(
    this.catalog.categories().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  protected readonly page = signal<Page<AdminProduct> | null>(null);
  protected readonly failed = signal(false);
  private filters: { q?: string; category?: string; active?: boolean | null; lowStock?: boolean } =
    {};
  private current = 0;

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.admin.products({ ...this.filters, page: this.current, size: 20 }).subscribe({
      next: (p) => this.page.set(p),
      error: () => this.failed.set(true),
    });
  }

  search(event: Event): void {
    event.preventDefault();
    const f = event.target as HTMLFormElement;
    const el = (n: string) => f.elements.namedItem(n) as HTMLInputElement;
    const active = el('active').value;
    this.filters = {
      q: el('q').value.trim() || undefined,
      category: el('category').value || undefined,
      active: active === '' ? null : active === 'true',
      lowStock: el('lowStock').checked || undefined,
    };
    this.current = 0;
    this.load();
  }

  goTo(page: number): void {
    this.current = page;
    this.load();
  }

  async deactivate(p: AdminProduct): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Désactiver ce produit ?',
      message: `« ${p.name} » ne sera plus visible en boutique. L'historique des commandes est conservé et vous pourrez le réactiver.`,
      confirmLabel: 'Désactiver',
      danger: true,
    });
    if (!ok) return;
    this.admin.deactivateProduct(p.id).subscribe({
      next: () => {
        this.ui.toast(`« ${p.name} » a été désactivé.`, 'info');
        this.catalog.invalidate();
        this.load();
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }
}
