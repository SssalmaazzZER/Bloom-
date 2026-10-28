import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, tap, throwError } from 'rxjs';
import { CartItemRequest, CartView } from '../../models/api.models';
import { API } from './catalog.service';

const STORAGE_KEY = 'bloome.cart.v1';

interface LocalLine extends CartItemRequest {
  clientKey: string;
}

/**
 * Cart state for the whole app.
 * - Signed-in customers: the cart lives on the server (persists across devices).
 * - Visitors: the lines (ids + quantities only, never prices) live in localStorage and are
 *   priced by POST /cart/quote. On login they are merged into the server cart.
 * Prices and totals always come from the API.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  private readonly _cart = signal<CartView | null>(null);
  private readonly _busy = signal(false);
  private authenticated = false;

  readonly cart = this._cart.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly count = computed(() => this._cart()?.itemCount ?? 0);

  initialize(authenticated: boolean): Observable<void> {
    this.authenticated = authenticated;
    if (authenticated && this.readLocal().length > 0) {
      return this.onLogin();
    }
    return this.refresh().pipe(
      map(() => void 0),
      catchError(() => of(void 0)),
    );
  }

  refresh(giftWrapping = false): Observable<CartView> {
    const request$ = this.authenticated
      ? this.http.get<CartView>(`${API}/cart`, { params: { giftWrapping } })
      : this.quoteLocal(giftWrapping);
    return request$.pipe(tap((cart) => this._cart.set(cart)));
  }

  /** Re-price with gift wrapping, without changing the displayed cart state. */
  preview(giftWrapping: boolean): Observable<CartView> {
    return this.authenticated
      ? this.http.get<CartView>(`${API}/cart`, { params: { giftWrapping } })
      : this.quoteLocal(giftWrapping);
  }

  add(item: CartItemRequest): Observable<CartView> {
    if (this.authenticated) {
      return this.track(this.http.post<CartView>(`${API}/cart/items`, item));
    }
    const lines = this.readLocal();
    const existing = item.productId ? lines.find((l) => l.productId === item.productId) : undefined;
    // Validate against the server before storing locally, so the visitor gets the same errors.
    const candidate: LocalLine[] = existing
      ? lines.map((l) => (l === existing ? { ...l, quantity: l.quantity + item.quantity } : l))
      : [...lines, { ...item, clientKey: this.newKey() }];
    return this.track(
      this.quote(candidate, false).pipe(
        tap((cart) => {
          const line = existing
            ? cart.items.find((l) => l.id === existing.clientKey)
            : cart.items[cart.items.length - 1];
          if (line?.issue) {
            throw {
              error: {
                code: 'INSUFFICIENT_STOCK',
                message:
                  line.maxQuantity > 0
                    ? `Il ne reste que ${line.maxQuantity} exemplaire(s) disponible(s).`
                    : `« ${line.name} » n'est plus disponible.`,
              },
            };
          }
          this.writeLocal(candidate);
        }),
      ),
    );
  }

  update(lineId: string, quantity: number): Observable<CartView> {
    if (this.authenticated) {
      return this.track(this.http.patch<CartView>(`${API}/cart/items/${lineId}`, { quantity }));
    }
    const lines = this.readLocal().map((l) => (l.clientKey === lineId ? { ...l, quantity } : l));
    this.writeLocal(lines);
    return this.track(this.quoteLocal(false));
  }

  remove(lineId: string): Observable<CartView> {
    if (this.authenticated) {
      return this.track(this.http.delete<CartView>(`${API}/cart/items/${lineId}`));
    }
    this.writeLocal(this.readLocal().filter((l) => l.clientKey !== lineId));
    return this.track(this.quoteLocal(false));
  }

  /** Merge the visitor cart into the customer cart, then forget the local copy. */
  onLogin(): Observable<void> {
    this.authenticated = true;
    const local = this.readLocal();
    const request$ = local.length
      ? this.http.post<CartView>(`${API}/cart/merge`, {
          items: local.map((line) => ({
            productId: line.productId,
            bouquet: line.bouquet,
            quantity: line.quantity,
          })),
        })
      : this.http.get<CartView>(`${API}/cart`);
    return request$.pipe(
      tap((cart) => {
        this.writeLocal([]);
        this._cart.set(cart);
      }),
      map(() => void 0),
      catchError(() => of(void 0)),
    );
  }

  onLogout(): void {
    this.authenticated = false;
    this._cart.set(null);
    this.refresh().subscribe({ error: () => undefined });
  }

  /** After a successful order the server cart is already empty. */
  markOrdered(): void {
    this._cart.update((c) =>
      c ? { ...c, items: [], itemCount: 0, subtotal: 0, total: 0, deliveryFee: 0 } : c,
    );
  }

  private track(request$: Observable<CartView>): Observable<CartView> {
    this._busy.set(true);
    return request$.pipe(
      tap((cart) => this._cart.set(cart)),
      catchError((err) => throwError(() => err)),
      finalize(() => this._busy.set(false)),
    );
  }

  private quoteLocal(giftWrapping: boolean): Observable<CartView> {
    return this.quote(this.readLocal(), giftWrapping);
  }

  private quote(lines: LocalLine[], giftWrapping: boolean): Observable<CartView> {
    return this.http.post<CartView>(
      `${API}/cart/quote`,
      { items: lines },
      { params: { giftWrapping } },
    );
  }

  private readLocal(): LocalLine[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const parsed: unknown = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed)
        ? (parsed as LocalLine[]).filter(
            (l) => typeof l?.quantity === 'number' && typeof l?.clientKey === 'string',
          )
        : [];
    } catch {
      return [];
    }
  }

  private writeLocal(lines: LocalLine[]): void {
    try {
      if (lines.length) localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* storage unavailable (private mode): the cart simply won't survive a refresh */
    }
  }

  private newKey(): string {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `k-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
