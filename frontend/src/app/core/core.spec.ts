import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  Router,
  UrlTree,
  provideRouter,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
} from '@angular/router';
import { convertToParamMap } from '@angular/router';
import { CartService } from './services/cart.service';
import { AuthService } from './services/auth.service';
import { adminGuard, authGuard } from './guards/auth.guards';
import { CartView, User } from '../models/api.models';
import { toQuery } from '../features/catalog/catalog.component';

const emptyCart = (overrides: Partial<CartView> = {}): CartView => ({
  items: [],
  itemCount: 0,
  subtotal: 0,
  deliveryFee: 0,
  giftWrappingFee: 0,
  discount: 0,
  total: 0,
  freeDeliveryThreshold: 500,
  amountToFreeDelivery: 500,
  checkoutReady: false,
  currency: 'MAD',
  ...overrides,
});

describe('CartService (visitor cart)', () => {
  let cart: CartService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    cart = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores only ids and quantities locally and asks the server for prices', () => {
    let result: CartView | undefined;
    cart.add({ productId: 7, quantity: 2 }).subscribe((c) => (result = c));
    const req = http.expectOne((r) => r.url === '/api/v1/cart/quote');
    expect(req.request.method).toBe('POST');
    const sent = req.request.body.items[0];
    expect(sent).toMatchObject({ productId: 7, quantity: 2 });
    expect(sent.price).toBeUndefined();
    req.flush(
      emptyCart({
        items: [
          {
            id: sent.clientKey,
            type: 'PRODUCT',
            productId: 7,
            name: 'Rose',
            unitPrice: 100,
            quantity: 2,
            lineTotal: 200,
            maxQuantity: 5,
          },
        ],
        itemCount: 2,
        subtotal: 200,
        total: 235,
      }),
    );
    expect(result?.total).toBe(235);
    expect(cart.count()).toBe(2);
    const stored = JSON.parse(localStorage.getItem('bloome.cart.v1')!);
    expect(stored).toHaveLength(1);
    expect(stored[0]).not.toHaveProperty('unitPrice');
  });

  it('updates quantities and re-prices', () => {
    localStorage.setItem(
      'bloome.cart.v1',
      JSON.stringify([{ clientKey: 'k1', productId: 3, quantity: 1 }]),
    );
    cart.update('k1', 4).subscribe();
    const req = http.expectOne('/api/v1/cart/quote?giftWrapping=false');
    expect(req.request.body.items[0].quantity).toBe(4);
    req.flush(emptyCart());
  });

  it('refuses to store a line the server flags as out of stock', () => {
    let error: unknown;
    cart.add({ productId: 9, quantity: 5 }).subscribe({ error: (e) => (error = e) });
    const req = http.expectOne((r) => r.url === '/api/v1/cart/quote');
    const key = req.request.body.items[0].clientKey;
    req.flush(
      emptyCart({
        items: [
          {
            id: key,
            type: 'PRODUCT',
            name: 'Pivoines',
            unitPrice: 650,
            quantity: 5,
            lineTotal: 3250,
            maxQuantity: 2,
            issue: 'QUANTITY_EXCEEDS_STOCK',
          },
        ],
      }),
    );
    expect(error).toBeTruthy();
    expect(localStorage.getItem('bloome.cart.v1')).toBeNull();
  });

  it('removes lines locally', () => {
    localStorage.setItem(
      'bloome.cart.v1',
      JSON.stringify([
        { clientKey: 'a', productId: 1, quantity: 1 },
        { clientKey: 'b', productId: 2, quantity: 1 },
      ]),
    );
    cart.remove('a').subscribe();
    http.expectOne((r) => r.url === '/api/v1/cart/quote').flush(emptyCart());
    expect(
      JSON.parse(localStorage.getItem('bloome.cart.v1')!).map(
        (l: { clientKey: string }) => l.clientKey,
      ),
    ).toEqual(['b']);
  });
});

describe('route guards', () => {
  const user = (role: User['role']): User => ({
    id: 1,
    email: 'x@y.ma',
    firstName: 'A',
    lastName: 'B',
    role,
    createdAt: '',
  });
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  const run = (guard: typeof authGuard, url: string) =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as Promise<boolean | UrlTree>;

  it('sends visitors to the login page with a return URL', async () => {
    const result = await run(authGuard, '/commande');
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result as UrlTree)).toBe('/connexion?returnUrl=%2Fcommande');
  });

  it('lets signed-in customers in but keeps them out of admin', async () => {
    auth.updateUser(user('CUSTOMER'));
    expect(await run(authGuard, '/compte')).toBe(true);
    const admin = await run(adminGuard, '/admin');
    expect(TestBed.inject(Router).serializeUrl(admin as UrlTree)).toBe('/');
    http.verify();
  });

  it('lets administrators into admin', async () => {
    auth.updateUser(user('ADMIN'));
    expect(await run(adminGuard, '/admin')).toBe(true);
  });
});

describe('catalog URL ↔ query mapping', () => {
  it('reads shareable French query parameters', () => {
    const q = toQuery(
      convertToParamMap({
        q: 'pivoine',
        categorie: ['bouquets', 'roses'],
        couleur: ['PINK', 'NOPE'],
        prixMin: '100',
        prixMax: '500',
        dispo: '1',
        tri: 'PRICE_ASC',
        page: '2',
      }),
    );
    expect(q).toMatchObject({
      q: 'pivoine',
      category: ['bouquets', 'roses'],
      color: ['PINK'],
      minPrice: 100,
      maxPrice: 500,
      inStock: true,
      sort: 'PRICE_ASC',
      page: 1,
    });
  });

  it('falls back to safe defaults for invalid values', () => {
    const q = toQuery(convertToParamMap({ prixMin: '-5', tri: 'DROP TABLE', page: 'abc' }));
    expect(q.minPrice).toBeUndefined();
    expect(q.sort).toBe('POPULARITY');
    expect(q.page).toBe(0);
  });
});
