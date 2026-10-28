import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import {
  BouquetOptions,
  CatalogFacets,
  Category,
  DeliveryOptions,
  Occasion,
  Page,
  ProductDetail,
  ProductQuery,
  ProductSummary,
  Review,
  ReviewPage,
} from '../../models/api.models';

export const API = '/api/v1';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  private categories$?: Observable<Category[]>;
  private occasions$?: Observable<Occasion[]>;
  private facets$?: Observable<CatalogFacets>;

  products(query: ProductQuery): Observable<Page<ProductSummary>> {
    let params = new HttpParams();
    const scalar: (keyof ProductQuery)[] = [
      'q',
      'minPrice',
      'maxPrice',
      'inStock',
      'sort',
      'page',
      'size',
    ];
    for (const key of scalar) {
      const value = query[key];
      if (value !== undefined && value !== null && value !== '' && value !== false) {
        params = params.set(key, String(value));
      }
    }
    for (const c of query.category ?? []) params = params.append('category', c);
    for (const o of query.occasion ?? []) params = params.append('occasion', o);
    for (const c of query.color ?? []) params = params.append('color', c);
    return this.http.get<Page<ProductSummary>>(`${API}/products`, { params });
  }

  product(idOrSlug: string): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${API}/products/${encodeURIComponent(idOrSlug)}`);
  }

  related(idOrSlug: string, limit = 4): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(
      `${API}/products/${encodeURIComponent(idOrSlug)}/related`,
      {
        params: { limit },
      },
    );
  }

  featured(limit = 8): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${API}/products/featured`, { params: { limit } });
  }

  bestsellers(limit = 8): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${API}/products/bestsellers`, { params: { limit } });
  }

  categories(): Observable<Category[]> {
    return (this.categories$ ??= this.http
      .get<Category[]>(`${API}/categories`)
      .pipe(shareReplay(1)));
  }

  occasions(): Observable<Occasion[]> {
    return (this.occasions$ ??= this.http.get<Occasion[]>(`${API}/occasions`).pipe(shareReplay(1)));
  }

  facets(): Observable<CatalogFacets> {
    return (this.facets$ ??= this.http
      .get<CatalogFacets>(`${API}/catalog/facets`)
      .pipe(shareReplay(1)));
  }

  /** Invalidate cached reference data after admin changes. */
  invalidate(): void {
    this.categories$ = undefined;
    this.occasions$ = undefined;
    this.facets$ = undefined;
  }

  bouquetOptions(): Observable<BouquetOptions> {
    return this.http.get<BouquetOptions>(`${API}/bouquet/options`);
  }

  deliveryOptions(): Observable<DeliveryOptions> {
    return this.http.get<DeliveryOptions>(`${API}/delivery/options`);
  }

  reviews(idOrSlug: string, page = 0): Observable<ReviewPage> {
    return this.http.get<ReviewPage>(`${API}/products/${encodeURIComponent(idOrSlug)}/reviews`, {
      params: { page, size: 5 },
    });
  }

  addReview(
    idOrSlug: string,
    body: { rating: number; title?: string; comment: string },
  ): Observable<Review> {
    return this.http.post<Review>(`${API}/products/${encodeURIComponent(idOrSlug)}/reviews`, body);
  }

  subscribe(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${API}/newsletter`, { email });
  }
}
