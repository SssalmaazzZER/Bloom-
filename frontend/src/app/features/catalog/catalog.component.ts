import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, map, merge, of, switchMap, tap } from 'rxjs';
import { CatalogService } from '../../core/services/catalog.service';
import {
  FlowerColor,
  Page,
  ProductQuery,
  ProductSort,
  ProductSummary,
} from '../../models/api.models';
import { COLOR_LABELS, COLOR_SWATCHES, SORT_LABELS } from '../../models/labels';
import { PaginationComponent } from '../../shared/components/pagination.component';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import {
  EmptyStateComponent,
  ErrorStateComponent,
  ProductGridSkeletonComponent,
} from '../../shared/components/states.component';
import { IconComponent } from '../../shared/components/icon.component';
import { formatMad } from '../../shared/pipes/mad.pipe';

const PAGE_SIZE = 12;
const SORTS = Object.keys(SORT_LABELS) as ProductSort[];
const COLORS = Object.keys(COLOR_LABELS) as FlowerColor[];

/** URL query parameters (French, shareable) ↔ API query. */
export function toQuery(params: ParamMap): ProductQuery {
  const num = (key: string) => {
    const v = params.get(key);
    const n = v === null || v === '' ? NaN : Number(v);
    return Number.isFinite(n) && n >= 0 ? n : undefined;
  };
  const sort = params.get('tri') as ProductSort | null;
  return {
    q: params.get('q')?.slice(0, 100) || undefined,
    category: params.getAll('categorie'),
    occasion: params.getAll('occasion'),
    color: params
      .getAll('couleur')
      .filter((c): c is FlowerColor => COLORS.includes(c as FlowerColor)),
    minPrice: num('prixMin'),
    maxPrice: num('prixMax'),
    inStock: params.get('dispo') === '1' || undefined,
    sort: sort && SORTS.includes(sort) ? sort : 'POPULARITY',
    page: Math.max(0, (num('page') ?? 1) - 1),
    size: PAGE_SIZE,
  };
}

interface Chip {
  label: string;
  param: string;
  value?: string;
}

@Component({
  selector: 'app-catalog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ProductCardComponent,
    PaginationComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    ProductGridSkeletonComponent,
    IconComponent,
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent {
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly sortLabels = SORT_LABELS;
  protected readonly sorts = SORTS;
  protected readonly colorLabels = COLOR_LABELS;
  protected readonly swatches = COLOR_SWATCHES;

  protected readonly facets = toSignal(this.catalog.facets().pipe(catchError(() => of(null))));
  protected readonly query = signal<ProductQuery>({});
  protected readonly result = signal<Page<ProductSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly filtersOpen = signal(false);
  private readonly retry$ = new Subject<void>();

  protected readonly title = computed(() => {
    const q = this.query();
    const cats = this.facets()?.categories ?? [];
    if (q.q) return `Résultats pour « ${q.q} »`;
    if (q.category?.length === 1)
      return cats.find((c) => c.slug === q.category![0])?.name ?? 'Boutique';
    if (q.occasion?.length === 1)
      return this.facets()?.occasions.find((o) => o.slug === q.occasion![0])?.name ?? 'Boutique';
    return 'Toute la boutique';
  });

  protected readonly chips = computed<Chip[]>(() => {
    const q = this.query();
    const f = this.facets();
    const chips: Chip[] = [];
    if (q.q) chips.push({ label: `« ${q.q} »`, param: 'q' });
    for (const c of q.category ?? [])
      chips.push({
        label: f?.categories.find((x) => x.slug === c)?.name ?? c,
        param: 'categorie',
        value: c,
      });
    for (const o of q.occasion ?? [])
      chips.push({
        label: f?.occasions.find((x) => x.slug === o)?.name ?? o,
        param: 'occasion',
        value: o,
      });
    for (const c of q.color ?? [])
      chips.push({ label: COLOR_LABELS[c], param: 'couleur', value: c });
    if (q.minPrice !== undefined)
      chips.push({ label: `Dès ${formatMad(q.minPrice)}`, param: 'prixMin' });
    if (q.maxPrice !== undefined)
      chips.push({ label: `Jusqu'à ${formatMad(q.maxPrice)}`, param: 'prixMax' });
    if (q.inStock) chips.push({ label: 'Disponibles', param: 'dispo' });
    return chips;
  });

  constructor() {
    merge(this.route.queryParamMap, this.retry$.pipe(map(() => this.route.snapshot.queryParamMap)))
      .pipe(
        tap((params) => {
          this.query.set(toQuery(params));
          this.loading.set(true);
          this.failed.set(false);
        }),
        debounceTime(30),
        switchMap(() =>
          this.catalog.products(this.query()).pipe(
            catchError(() => {
              this.failed.set(true);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.result.set(page);
        this.loading.set(false);
      });
  }

  /** Writes a change to the URL; the URL drives the request (refreshable, shareable). */
  update(changes: Record<string, string | string[] | null>, keepPage = false): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: keepPage ? changes : { ...changes, page: null },
      queryParamsHandling: 'merge',
    });
  }

  isChecked(param: 'categorie' | 'occasion' | 'couleur', value: string): boolean {
    const q = this.query();
    const list = param === 'categorie' ? q.category : param === 'occasion' ? q.occasion : q.color;
    return (list as string[] | undefined)?.includes(value) ?? false;
  }

  toggle(param: 'categorie' | 'occasion' | 'couleur', value: string): void {
    const current = this.route.snapshot.queryParamMap.getAll(param);
    const next = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
    this.update({ [param]: next.length ? next : null });
  }

  setPrice(param: 'prixMin' | 'prixMax', event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    const n = Number(raw);
    this.update({
      [param]: raw === '' || !Number.isFinite(n) || n < 0 ? null : String(Math.round(n)),
    });
  }

  setSort(event: Event): void {
    this.update({ tri: (event.target as HTMLSelectElement).value });
  }

  setInStock(event: Event): void {
    this.update({ dispo: (event.target as HTMLInputElement).checked ? '1' : null });
  }

  search(event: Event): void {
    event.preventDefault();
    const input = (event.target as HTMLFormElement).elements.namedItem('q') as HTMLInputElement;
    this.update({ q: input.value.trim() || null });
  }

  removeChip(chip: Chip): void {
    if (chip.value) {
      const current = this.route.snapshot.queryParamMap
        .getAll(chip.param)
        .filter((v) => v !== chip.value);
      this.update({ [chip.param]: current.length ? current : null });
    } else {
      this.update({ [chip.param]: null });
    }
  }

  clearAll(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  goToPage(page: number): void {
    this.update({ page: page > 0 ? String(page + 1) : null }, true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  retry(): void {
    this.retry$.next();
  }
}
