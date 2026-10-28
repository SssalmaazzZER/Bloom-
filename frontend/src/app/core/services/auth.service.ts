import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, firstValueFrom, map, of, switchMap, tap } from 'rxjs';
import { User } from '../../models/api.models';
import { API } from './catalog.service';
import { CartService } from './cart.service';
import { AccountService } from './account.service';

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
}

/**
 * Session-cookie authentication. The browser keeps the HttpOnly session cookie; this service
 * only mirrors "who is signed in" for the UI. Authorisation is always enforced by the API.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);
  private readonly account = inject(AccountService);

  private readonly _user = signal<User | null>(null);
  private readonly _ready = signal(false);
  private initPromise?: Promise<void>;

  readonly user = this._user.asReadonly();
  readonly ready = this._ready.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Called once at start-up: restores the session (and receives the CSRF cookie). */
  init(): Promise<void> {
    this.initPromise = firstValueFrom(
      this.http.get<User>(`${API}/auth/me`).pipe(
        catchError(() => of(null)),
        tap((user) => {
          this._user.set(user);
          if (user) this.account.wishlist().subscribe({ error: () => undefined });
        }),
        switchMap((user) => this.cart.initialize(user !== null)),
        catchError(() => of(void 0)),
        map(() => void 0),
      ),
    ).finally(() => this._ready.set(true));
    return this.initPromise;
  }

  /** Waits until the initial session check finished (used by guards). */
  whenReady(): Promise<void> {
    return this.initPromise ?? Promise.resolve();
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>(`${API}/auth/login`, { email, password }).pipe(
      tap((user) => this._user.set(user)),
      switchMap((user) => this.cart.onLogin().pipe(map(() => user))),
    );
  }

  register(payload: RegisterPayload): Observable<User> {
    return this.http.post<User>(`${API}/auth/register`, payload).pipe(
      tap((user) => this._user.set(user)),
      switchMap((user) => this.cart.onLogin().pipe(map(() => user))),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API}/auth/logout`, {}).pipe(
      catchError(() => of(void 0)),
      tap(() => this.clearSession()),
    );
  }

  /** Local sign-out (e.g. after the server reported the session expired). */
  clearSession(): void {
    this._user.set(null);
    this.account.clearWishlistState();
    this.cart.onLogout();
  }

  updateUser(user: User): void {
    this._user.set(user);
  }
}
