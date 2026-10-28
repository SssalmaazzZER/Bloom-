import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/services/catalog.service';
import { CartService } from '../../core/services/cart.service';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { UiService } from '../../core/services/ui.service';
import { apiError, errorMessage } from '../../core/errors';
import { ProductDetail, ProductSummary, ReviewPage } from '../../models/api.models';
import { COLOR_LABELS, COLOR_SWATCHES } from '../../models/labels';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { QuantitySelectorComponent } from '../../shared/components/quantity-selector.component';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import { RatingComponent } from '../../shared/components/rating.component';
import { IconComponent } from '../../shared/components/icon.component';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-product',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MadPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    QuantitySelectorComponent,
    ProductCardComponent,
    RatingComponent,
    IconComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  templateUrl: './product.component.html',
  styleUrl: './product.component.scss',
})
export class ProductComponent {
  /** Bound from the route parameter :slug. */
  readonly slug = input.required<string>();

  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly account = inject(AccountService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(UiService);
  private readonly titleService = inject(Title);

  protected readonly colorLabels = COLOR_LABELS;
  protected readonly swatches = COLOR_SWATCHES;

  protected readonly product = signal<ProductDetail | null>(null);
  protected readonly status = signal<'loading' | 'ready' | 'notfound' | 'error'>('loading');
  protected readonly related = signal<ProductSummary[]>([]);
  protected readonly reviews = signal<ReviewPage | null>(null);
  protected readonly activeImage = signal(0);
  protected readonly quantity = signal(1);
  protected readonly adding = signal(false);
  protected readonly reviewSending = signal(false);
  protected readonly reviewError = signal('');

  protected readonly wished = computed(() => {
    const p = this.product();
    return p ? this.account.wishlistIds().has(p.id) : false;
  });

  protected readonly reviewForm = new FormGroup({
    rating: new FormControl(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),
    title: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    comment: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10), Validators.maxLength(2000)],
    }),
  });

  constructor() {
    effect(() => this.load(this.slug()));
  }

  load(slug: string): void {
    this.status.set('loading');
    this.activeImage.set(0);
    this.quantity.set(1);
    this.catalog.product(slug).subscribe({
      next: (p) => {
        this.product.set(p);
        this.status.set('ready');
        this.titleService.setTitle(`${p.name} | Bloomé`);
        this.catalog
          .related(slug, 4)
          .subscribe({ next: (r) => this.related.set(r), error: () => this.related.set([]) });
        this.loadReviews();
      },
      error: (err) => {
        this.product.set(null);
        this.status.set(apiError(err).code === 'NOT_FOUND' ? 'notfound' : 'error');
      },
    });
  }

  loadReviews(page = 0): void {
    this.catalog.reviews(this.slug(), page).subscribe({
      next: (r) => this.reviews.set(r),
      error: () => this.reviews.set(null),
    });
  }

  addToCart(): void {
    const p = this.product();
    if (!p || !p.inStock) return;
    this.adding.set(true);
    this.cart.add({ productId: p.id, quantity: this.quantity() }).subscribe({
      next: () => {
        this.adding.set(false);
        this.ui.toast(`${this.quantity()} × « ${p.name} » ajouté à votre panier.`, 'success', {
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
    const p = this.product();
    if (!p) return;
    const req$ = this.wished()
      ? this.account.removeFromWishlist(p.id)
      : this.account.addToWishlist(p.id);
    const adding = !this.wished();
    req$.subscribe({
      next: () =>
        this.ui.toast(adding ? 'Ajouté à vos favoris.' : 'Retiré de vos favoris.', 'info'),
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }

  submitReview(): void {
    this.reviewForm.markAllAsTouched();
    if (this.reviewForm.invalid) return;
    this.reviewSending.set(true);
    this.reviewError.set('');
    const v = this.reviewForm.getRawValue();
    this.catalog
      .addReview(this.slug(), {
        rating: Number(v.rating),
        title: v.title || undefined,
        comment: v.comment,
      })
      .subscribe({
        next: () => {
          this.reviewSending.set(false);
          this.reviewForm.reset({ rating: 5, title: '', comment: '' });
          this.ui.toast('Merci ! Votre avis a été publié.');
          this.loadReviews();
        },
        error: (err) => {
          this.reviewSending.set(false);
          this.reviewError.set(errorMessage(err));
        },
      });
  }
}
