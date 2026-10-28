import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { CheckoutComponent } from './checkout/checkout.component';
import { CatalogComponent } from './catalog/catalog.component';
import { CartView, DeliveryOptions, Page, ProductSummary } from '../models/api.models';

const delivery: DeliveryOptions = {
  cities: ['Rabat', 'Salé'],
  slots: [
    { code: 'MORNING', label: 'Matin (9h – 12h)', startHour: 9, endHour: 12 },
    { code: 'EVENING', label: 'Soirée (17h – 20h)', startHour: 17, endHour: 20 },
  ],
  earliestDate: '2026-10-28',
  earliestDateSlots: ['MORNING', 'EVENING'],
  latestDate: '2026-11-27',
  deliveryFee: 35,
  freeDeliveryThreshold: 500,
  giftWrappingFee: 25,
  sameDayCutoffHour: 14,
  currency: 'MAD',
};

const cart: CartView = {
  items: [
    {
      id: '1',
      type: 'PRODUCT',
      productId: 1,
      slug: 'x',
      name: 'Velvet Roses',
      unitPrice: 590,
      quantity: 1,
      lineTotal: 590,
      maxQuantity: 5,
    },
  ],
  itemCount: 1,
  subtotal: 590,
  deliveryFee: 0,
  giftWrappingFee: 0,
  discount: 0,
  total: 590,
  freeDeliveryThreshold: 500,
  amountToFreeDelivery: 0,
  checkoutReady: true,
  currency: 'MAD',
};

describe('CheckoutComponent', () => {
  let fixture: ComponentFixture<CheckoutComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CheckoutComponent);
    fixture.detectChanges();
    http.expectOne('/api/v1/account/addresses').flush([]);
    http.expectOne('/api/v1/delivery/options').flush(delivery);
    http.expectOne((r) => r.url === '/api/v1/cart/quote').flush(cart);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('shows the server-computed summary', () => {
    expect(fixture.nativeElement.querySelector('.summary').textContent).toContain('590');
  });

  it('blocks submission and shows errors when required fields are missing', () => {
    (fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectNone('/api/v1/orders');
    const errors = fixture.nativeElement.querySelectorAll('.error');
    expect(errors.length).toBeGreaterThan(2);
    expect(fixture.nativeElement.textContent).toContain('Ce champ est requis.');
  });

  it('sends a complete cash-on-delivery order with an idempotency key and the expected total', () => {
    const form = (fixture.componentInstance as unknown as { form: { patchValue(v: object): void } })
      .form;
    form.patchValue({
      customerName: 'Yasmine Alaoui',
      customerEmail: 'yasmine@example.com',
      customerPhone: '0612345678',
      addressLine1: '12 rue des Orangers',
      city: 'Rabat',
      deliveryDate: '2026-10-29',
      deliverySlot: 'MORNING',
    });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement).click();
    const req = http.expectOne('/api/v1/orders');
    expect(req.request.body).toMatchObject({
      paymentMethod: 'CASH_ON_DELIVERY',
      city: 'Rabat',
      expectedTotal: 590,
      giftWrapping: false,
    });
    expect(req.request.body.idempotencyKey.length).toBeGreaterThanOrEqual(8);
    expect(req.request.body.recipientName).toBeUndefined();
    req.flush(
      { code: 'PRICE_CHANGED', message: 'Le montant a changé.' },
      { status: 409, statusText: 'Conflict' },
    );
    http
      .match(() => true)
      .forEach((r) => r.flush(r.request.url.includes('delivery') ? delivery : cart));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.alert').textContent).toContain(
      'Le montant a changé.',
    );
  });
});

describe('CatalogComponent', () => {
  let http: HttpTestingController;
  const page = (items: Partial<ProductSummary>[]): Page<ProductSummary> => ({
    content: items as ProductSummary[],
    page: 0,
    size: 12,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'boutique', component: CatalogComponent }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('requests products with the filters from the URL and renders results, then an empty state', async () => {
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/boutique?q=rose&categorie=bouquets&prixMax=500');
    const fixture = TestBed.createComponent(CatalogComponent);
    fixture.detectChanges();
    http
      .expectOne('/api/v1/catalog/facets')
      .flush({ minPrice: 100, maxPrice: 700, colors: ['RED'], categories: [], occasions: [] });
    await new Promise((r) => setTimeout(r, 60));
    const req = http.expectOne((r) => r.url === '/api/v1/products');
    expect(req.request.params.get('q')).toBe('rose');
    expect(req.request.params.getAll('category')).toEqual(['bouquets']);
    expect(req.request.params.get('maxPrice')).toBe('500');
    req.flush(
      page([
        {
          id: 1,
          slug: 'a',
          name: 'Velvet Roses',
          price: 590,
          categoryName: 'Roses',
          inStock: true,
          availableQuantity: 3,
          colors: [],
          shortDescription: '',
        },
      ]),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('app-product-card').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Résultats pour « rose »');

    fixture.componentInstance.update({ q: 'introuvable' });
    await new Promise((r) => setTimeout(r, 60));
    http.expectOne((r) => r.url === '/api/v1/products').flush(page([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune création ne correspond');
  });

  it('shows an error state when the API fails', async () => {
    await TestBed.inject(Router).navigateByUrl('/boutique');
    const fixture = TestBed.createComponent(CatalogComponent);
    fixture.detectChanges();
    http
      .expectOne('/api/v1/catalog/facets')
      .flush({ minPrice: 0, maxPrice: 0, colors: [], categories: [], occasions: [] });
    await new Promise((r) => setTimeout(r, 60));
    http
      .expectOne((r) => r.url === '/api/v1/products')
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]')).not.toBeNull();
  });
});
