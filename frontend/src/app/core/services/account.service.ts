import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Address, AddressRequest, ProductSummary, User } from '../../models/api.models';
import { API } from './catalog.service';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  /** Ids of wishlisted products, for the heart toggles on product cards. */
  readonly wishlistIds = signal<Set<number>>(new Set());

  updateProfile(body: { firstName: string; lastName: string; phone?: string }): Observable<User> {
    return this.http.put<User>(`${API}/account/profile`, body);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${API}/account/password`, { currentPassword, newPassword });
  }

  addresses(): Observable<Address[]> {
    return this.http.get<Address[]>(`${API}/account/addresses`);
  }

  createAddress(body: AddressRequest): Observable<Address> {
    return this.http.post<Address>(`${API}/account/addresses`, body);
  }

  updateAddress(id: number, body: AddressRequest): Observable<Address> {
    return this.http.put<Address>(`${API}/account/addresses/${id}`, body);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/account/addresses/${id}`);
  }

  wishlist(): Observable<ProductSummary[]> {
    return this.http
      .get<ProductSummary[]>(`${API}/wishlist`)
      .pipe(tap((items) => this.syncIds(items)));
  }

  addToWishlist(productId: number): Observable<ProductSummary[]> {
    return this.http
      .post<ProductSummary[]>(`${API}/wishlist/items/${productId}`, {})
      .pipe(tap((items) => this.syncIds(items)));
  }

  removeFromWishlist(productId: number): Observable<ProductSummary[]> {
    return this.http
      .delete<ProductSummary[]>(`${API}/wishlist/items/${productId}`)
      .pipe(tap((items) => this.syncIds(items)));
  }

  clearWishlistState(): void {
    this.wishlistIds.set(new Set());
  }

  private syncIds(items: ProductSummary[]): void {
    this.wishlistIds.set(new Set(items.map((p) => p.id)));
  }
}
