import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormControl } from '@angular/forms';
import { formatMad } from './pipes/mad.pipe';
import { ImgSizePipe } from './pipes/labels.pipes';
import { QuantitySelectorComponent } from './components/quantity-selector.component';
import { ProductCardComponent } from './components/product-card.component';
import { EmptyStateComponent, ErrorStateComponent } from './components/states.component';
import { phoneValidator, strongPassword } from './validators/validators';
import { safeReturnUrl } from '../features/auth/login.component';
import { ProductSummary } from '../models/api.models';

const product = (overrides: Partial<ProductSummary> = {}): ProductSummary => ({
  id: 1,
  slug: 'velvet-roses',
  name: 'Velvet Roses',
  shortDescription: 'Une brassée de roses rouges.',
  price: 590,
  compareAtPrice: 650,
  imageUrl: 'https://images.unsplash.com/photo-1?w=1200',
  categorySlug: 'roses',
  categoryName: 'Roses',
  inStock: true,
  lowStock: false,
  availableQuantity: 6,
  featured: true,
  bestseller: true,
  colors: ['RED'],
  ratingCount: 0,
  createdAt: '2026-10-01T00:00:00Z',
  ...overrides,
});

describe('price formatting', () => {
  it('formats whole and decimal dirham amounts in French', () => {
    expect(formatMad(590)).toBe('590\u00a0MAD');
    expect(formatMad(315.5)).toBe('315,50\u00a0MAD');
    expect(formatMad(1425)).toMatch(/^1\s425\u00a0MAD$/);
    expect(formatMad(null)).toBe('—');
  });

  it('rewrites Unsplash widths for thumbnails only', () => {
    const pipe = new ImgSizePipe();
    expect(
      pipe.transform('https://images.unsplash.com/photo-1?auto=format&w=1200&q=80', 320),
    ).toContain('w=320');
    expect(pipe.transform('/assets/rose.jpg', 320)).toBe('/assets/rose.jpg');
    expect(pipe.transform(undefined, 320)).toBeNull();
  });
});

describe('form validators', () => {
  it('accepts Moroccan phone numbers and rejects others', () => {
    for (const ok of ['0612345678', '06 12 34 56 78', '+212612345678', '0537-00-00-00', '']) {
      expect(phoneValidator(new FormControl(ok))).toBeNull();
    }
    for (const bad of ['12345', '0812345678', '06123456', 'abc']) {
      expect(phoneValidator(new FormControl(bad))).toEqual({ phone: true });
    }
  });

  it('requires 8+ characters with a letter and a digit', () => {
    expect(strongPassword(new FormControl('Fleurs2026'))).toBeNull();
    expect(strongPassword(new FormControl('short1'))).toEqual({ weak: true });
    expect(strongPassword(new FormControl('onlyletters'))).toEqual({ weak: true });
    expect(strongPassword(new FormControl('12345678'))).toEqual({ weak: true });
  });

  it('only allows same-site return URLs after login', () => {
    expect(safeReturnUrl('/commande')).toBe('/commande');
    expect(safeReturnUrl('//evil.example')).toBeNull();
    expect(safeReturnUrl('https://evil.example')).toBeNull();
    expect(safeReturnUrl(undefined)).toBeNull();
  });
});

describe('QuantitySelectorComponent', () => {
  let fixture: ComponentFixture<QuantitySelectorComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(QuantitySelectorComponent);
    fixture.componentRef.setInput('min', 1);
    fixture.componentRef.setInput('max', 3);
    fixture.detectChanges();
  });

  it('increments and decrements within bounds', () => {
    const [minus, plus] = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;
    expect(minus.disabled).toBe(true);
    plus.click();
    plus.click();
    plus.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.value()).toBe(3);
    expect(plus.disabled).toBe(true);
    minus.click();
    expect(fixture.componentInstance.value()).toBe(2);
  });

  it('clamps typed values', () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = '99';
    input.dispatchEvent(new Event('change'));
    expect(fixture.componentInstance.value()).toBe(3);
    input.value = '-4';
    input.dispatchEvent(new Event('change'));
    expect(fixture.componentInstance.value()).toBe(1);
  });
});

describe('ProductCardComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('renders name, category and prices', () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product());
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h3')?.textContent).toContain('Velvet Roses');
    expect(el.textContent).toContain('Roses');
    expect(el.querySelector('.price')?.textContent).toContain('590');
    expect(el.querySelector('.price-was')?.textContent).toContain('650');
    expect(el.querySelector<HTMLButtonElement>('button.add')?.disabled).toBe(false);
  });

  it('disables purchase for sold-out products', () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product({ inStock: false, availableQuantity: 0 }));
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button.add') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Épuisé');
  });
});

describe('empty and error states', () => {
  it('shows the empty-state message', () => {
    const fixture = TestBed.createComponent(EmptyStateComponent);
    fixture.componentRef.setInput('title', 'Aucune création ne correspond');
    fixture.componentRef.setInput('message', 'Retirez un filtre.');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune création ne correspond');
    expect(fixture.nativeElement.querySelector('[role=status]')).not.toBeNull();
  });

  it('emits retry from the error state', () => {
    const fixture = TestBed.createComponent(ErrorStateComponent);
    let retried = false;
    fixture.componentInstance.retry.subscribe(() => (retried = true));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    expect(retried).toBe(true);
    expect(fixture.nativeElement.querySelector('[role=alert]')).not.toBeNull();
  });
});
