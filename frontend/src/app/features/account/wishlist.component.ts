import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { ProductSummary } from '../../models/api.models';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-wishlist',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductCardComponent, EmptyStateComponent, ErrorStateComponent],
  template: `
    <h2>Mes favoris</h2>
    @if (failed()) {
      <app-error-state title="Vos favoris n'ont pas pu être chargés" (retry)="load()" />
    } @else if (items(); as list) {
      @if (list.length === 0) {
        <app-empty-state
          icon="heart"
          title="Aucun favori pour l'instant"
          message="Touchez le cœur sur une création pour la retrouver ici."
        >
          <a class="btn" routerLink="/boutique">Parcourir la boutique</a>
        </app-empty-state>
      } @else {
        <div class="grid">
          @for (p of visible(list); track p.id) {
            <app-product-card [product]="p" />
          }
        </div>
      }
    } @else {
      <div class="skeleton" style="height: 300px"></div>
    }
  `,
  styles: `
    h2 {
      font-size: 1.6rem;
    }
    .grid {
      display: grid;
      gap: 20px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 220px), 1fr));
    }
  `,
})
export class WishlistComponent {
  private readonly account = inject(AccountService);
  protected readonly items = signal<ProductSummary[] | null>(null);
  protected readonly failed = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.account
      .wishlist()
      .subscribe({ next: (i) => this.items.set(i), error: () => this.failed.set(true) });
  }

  /** Hides products un-hearted from this page without waiting for a reload. */
  protected visible(list: ProductSummary[]): ProductSummary[] {
    const ids = this.account.wishlistIds();
    return list.filter((p) => ids.has(p.id));
  }
}
