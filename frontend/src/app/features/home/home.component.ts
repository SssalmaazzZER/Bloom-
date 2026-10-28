import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { CatalogService } from '../../core/services/catalog.service';
import { ProductSummary } from '../../models/api.models';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import {
  ErrorStateComponent,
  ProductGridSkeletonComponent,
} from '../../shared/components/states.component';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { IconComponent } from '../../shared/components/icon.component';

const IMG = (id: string, w = 1400) =>
  `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=80`;

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ProductCardComponent,
    ProductGridSkeletonComponent,
    ErrorStateComponent,
    ImgFallbackDirective,
    ImgSizePipe,
    IconComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private readonly catalog = inject(CatalogService);

  protected readonly heroImage = IMG('1487530811176-3780de880c2d', 1400);
  protected readonly heroInset = IMG('1567696153798-9111f9cd3d0d', 500);
  protected readonly seasonImage = IMG('1444021465936-c6ca81d39b84', 1200);
  protected readonly storyImage = IMG('1563241527-3004b7be0ffd', 1000);

  protected readonly categories = toSignal(
    this.catalog.categories().pipe(catchError(() => of([]))),
  );
  protected readonly occasions = toSignal(this.catalog.occasions().pipe(catchError(() => of([]))));
  protected readonly delivery = toSignal(
    this.catalog.deliveryOptions().pipe(catchError(() => of(null))),
  );

  protected readonly bestsellers = signal<ProductSummary[] | null>(null);
  protected readonly bestsellersError = signal(false);

  /** Illustrative quotes — clearly labelled as demo content in the template. */
  protected readonly testimonials = [
    {
      quote:
        "Le bouquet est arrivé le jour même chez ma mère à Kénitra, encore plus beau qu'en photo. Elle l'a gardé presque deux semaines.",
      author: 'Salma',
      city: 'Rabat',
    },
    {
      quote:
        "J'ai composé mon propre bouquet de pivoines et de fougères pour notre anniversaire. Le message manuscrit était un vrai plus.",
      author: 'Mehdi',
      city: 'Casablanca',
    },
    {
      quote:
        'Livraison soignée, emballage magnifique et un monstera en pleine forme. Mon nouveau réflexe pour offrir.',
      author: 'Inès',
      city: 'Salé',
    },
  ];

  constructor() {
    this.loadBestsellers();
  }

  loadBestsellers(): void {
    this.bestsellersError.set(false);
    this.bestsellers.set(null);
    this.catalog.bestsellers(4).subscribe({
      next: (items) => this.bestsellers.set(items),
      error: () => this.bestsellersError.set(true),
    });
  }
}
