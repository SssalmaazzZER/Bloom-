import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/services/catalog.service';
import { Occasion } from '../../models/api.models';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-occasions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ImgFallbackDirective, ImgSizePipe, ErrorStateComponent],
  template: `
    <div class="container">
      <header class="page-title">
        <h1>Des fleurs pour chaque moment</h1>
        <p class="lede">
          Choisissez l'occasion, nous vous montrons les créations qui lui correspondent.
        </p>
      </header>
      @if (failed()) {
        <app-error-state (retry)="load()" />
      } @else {
        <ul class="grid">
          @for (o of occasions(); track o.id) {
            <li>
              <a routerLink="/boutique" [queryParams]="{ occasion: o.slug }" class="tile">
                <span class="img"
                  ><img appImgFallback [src]="o.imageUrl | imgSize: 700" alt="" loading="lazy"
                /></span>
                <span class="name">{{ o.name }}</span>
                <span class="desc">{{ o.description }}</span>
              </a>
            </li>
          } @empty {
            @for (i of [1, 2, 3]; track i) {
              <li><div class="skeleton" style="aspect-ratio: 4/5"></div></li>
            }
          }
        </ul>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      padding-block-end: 96px;
    }
    .grid {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 28px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
    }
    .tile {
      display: grid;
      gap: 6px;
      text-decoration: none;
      color: var(--ink);
      &:hover img {
        transform: scale(1.04);
      }
      &:hover .name {
        color: var(--rose-ink);
      }
    }
    .img {
      display: block;
      overflow: hidden;
      aspect-ratio: 4 / 5;
      border-radius: 999px 999px 18px 18px;
      background: var(--blush);
      margin-block-end: 10px;
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.6s var(--ease);
      }
    }
    .name {
      font-family: var(--font-display);
      font-size: 1.5rem;
      color: var(--burgundy);
    }
    .desc {
      color: var(--ink-soft);
    }
  `,
})
export class OccasionsComponent {
  private readonly catalog = inject(CatalogService);
  protected readonly occasions = signal<Occasion[]>([]);
  protected readonly failed = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.catalog
      .occasions()
      .subscribe({ next: (o) => this.occasions.set(o), error: () => this.failed.set(true) });
  }
}
