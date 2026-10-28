import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductSummary } from '../../models/api.models';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { ImgFallbackDirective } from '../directives/img-fallback.directive';
import { MadPipe } from '../pipes/mad.pipe';
import { ImgSizePipe } from '../pipes/labels.pipes';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-product-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MadPipe, ImgSizePipe, ImgFallbackDirective, IconComponent],
  template: `
    @let p = product();
    <article class="card-product" [class.soldout]="!p.inStock">
      <a class="media" [routerLink]="['/produit', p.slug]" [attr.aria-label]="p.name">
        <img
          appImgFallback
          [src]="p.imageUrl | imgSize: 640"
          [alt]="p.name"
          loading="lazy"
          width="640"
          height="800"
        />
        @if (!p.inStock) {
          <span class="flag badge danger">Épuisé</span>
        } @else if (p.lowStock) {
          <span class="flag badge warn">Plus que {{ p.availableQuantity }}</span>
        } @else if (p.compareAtPrice) {
          <span class="flag badge ink">Prix doux</span>
        }
      </a>
      <button
        type="button"
        class="wish"
        [class.on]="wished()"
        [attr.aria-pressed]="wished()"
        [attr.aria-label]="
          wished() ? 'Retirer ' + p.name + ' des favoris' : 'Ajouter ' + p.name + ' aux favoris'
        "
        (click)="toggleWish()"
      >
        <app-icon name="heart" [size]="20" />
      </button>
      <div class="body">
        <p class="cat">{{ p.categoryName }}</p>
        <h3>
          <a [routerLink]="['/produit', p.slug]">{{ p.name }}</a>
        </h3>
        <p class="desc">{{ p.shortDescription }}</p>
        <div class="foot">
          <span class="price">
            {{ p.price | mad }}
            @if (p.compareAtPrice) {
              <span class="price-was"
                ><span class="visually-hidden">au lieu de </span>{{ p.compareAtPrice | mad }}</span
              >
            }
          </span>
          <button
            type="button"
            class="btn btn-sm add"
            [disabled]="!p.inStock || adding()"
            (click)="addToCart()"
            [attr.aria-label]="'Ajouter ' + p.name + ' au panier'"
          >
            {{ p.inStock ? 'Ajouter' : 'Épuisé' }}
          </button>
        </div>
      </div>
    </article>
  `,
  styles: `
    .card-product {
      position: relative;
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: var(--radius-lg);
      overflow: hidden;
      transition: border-color 0.25s var(--ease);
      &:hover {
        border-color: var(--blush-deep);
      }
      &:hover img {
        transform: scale(1.04);
      }
    }
    .media {
      position: relative;
      display: block;
      aspect-ratio: 4 / 5;
      overflow: hidden;
      background: var(--blush);
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.6s var(--ease);
      }
    }
    .soldout .media img {
      filter: grayscale(0.6) opacity(0.75);
    }
    .flag {
      position: absolute;
      inset-block-start: 14px;
      inset-inline-start: 14px;
    }
    .wish {
      position: absolute;
      inset-block-start: 10px;
      inset-inline-end: 10px;
      display: grid;
      place-items: center;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 0;
      background: rgba(255, 249, 243, 0.9);
      color: var(--burgundy);
      cursor: pointer;
      transition:
        background 0.2s var(--ease),
        color 0.2s var(--ease);
      &:hover {
        background: var(--white);
      }
      &.on {
        color: var(--rose);
      }
      &.on ::ng-deep path {
        fill: currentColor;
      }
    }
    .body {
      display: flex;
      flex-direction: column;
      flex: 1;
      padding: 16px 18px 18px;
    }
    .cat {
      margin: 0 0 2px;
      font-size: 0.82rem;
      color: var(--sage-ink);
      font-weight: 600;
    }
    h3 {
      font-size: 1.28rem;
      margin: 0 0 6px;
      a {
        color: inherit;
        text-decoration: none;
      }
      a:hover {
        color: var(--rose-ink);
      }
    }
    .desc {
      font-size: 0.92rem;
      color: var(--ink-soft);
      margin: 0 0 14px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .foot {
      margin-block-start: auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      flex-wrap: wrap;
    }
    .price {
      font-size: 1.05rem;
    }
  `,
})
export class ProductCardComponent {
  readonly product = input.required<ProductSummary>();

  private readonly cart = inject(CartService);
  private readonly ui = inject(UiService);
  private readonly auth = inject(AuthService);
  private readonly account = inject(AccountService);

  protected readonly adding = signal(false);
  protected readonly wished = computed(() => this.account.wishlistIds().has(this.product().id));

  addToCart(): void {
    const p = this.product();
    this.adding.set(true);
    this.cart.add({ productId: p.id, quantity: 1 }).subscribe({
      next: () => {
        this.adding.set(false);
        this.ui.toast(`« ${p.name} » a été ajouté à votre panier.`, 'success', {
          label: 'Voir le panier',
          link: '/panier',
        });
      },
      error: (err) => {
        this.adding.set(false);
        this.ui.error(errorMessage(err));
      },
    });
  }

  toggleWish(): void {
    if (!this.auth.isLoggedIn()) {
      this.ui.toast('Connectez-vous pour enregistrer vos favoris.', 'info', {
        label: 'Se connecter',
        link: '/connexion',
      });
      return;
    }
    const p = this.product();
    const request$ = this.wished()
      ? this.account.removeFromWishlist(p.id)
      : this.account.addToWishlist(p.id);
    const adding = !this.wished();
    request$.subscribe({
      next: () =>
        this.ui.toast(
          adding ? `« ${p.name} » ajouté à vos favoris.` : `« ${p.name} » retiré de vos favoris.`,
          'info',
        ),
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }
}
