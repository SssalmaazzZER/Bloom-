import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AdminService } from '../core/services/admin.service';
import { CatalogService } from '../core/services/catalog.service';
import { UiService } from '../core/services/ui.service';
import { errorMessage, fieldErrors } from '../core/errors';
import { AdminProduct, AdminProductRequest, FlowerColor } from '../models/api.models';
import { COLOR_LABELS, COLOR_SWATCHES } from '../models/labels';
import { ImgFallbackDirective } from '../shared/directives/img-fallback.directive';
import { ImgSizePipe } from '../shared/pipes/labels.pipes';
import { IconComponent } from '../shared/components/icon.component';

const URL_RE = /^(https:\/\/[^\s/$.?#].[^\s]*|\/assets\/[A-Za-z0-9/_\-.]+)$/;

function imageGroup(url = '', altText = '') {
  return new FormGroup({
    url: new FormControl(url, {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(URL_RE), Validators.maxLength(500)],
    }),
    altText: new FormControl(altText, {
      nonNullable: true,
      validators: [Validators.maxLength(200)],
    }),
  });
}

@Component({
  selector: 'app-product-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ImgFallbackDirective, ImgSizePipe, IconComponent],
  templateUrl: './product-form.component.html',
  styleUrls: ['./admin-shared.scss', './product-form.component.scss'],
})
export class ProductFormComponent {
  /** Route param; undefined on /admin/produits/nouveau. */
  readonly id = input<string>();

  private readonly admin = inject(AdminService);
  private readonly catalog = inject(CatalogService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  protected readonly categories = toSignal(
    this.catalog.categories().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  protected readonly occasions = toSignal(this.catalog.occasions().pipe(catchError(() => of([]))), {
    initialValue: [],
  });
  protected readonly colors = Object.keys(COLOR_LABELS) as FlowerColor[];
  protected readonly colorLabels = COLOR_LABELS;
  protected readonly swatches = COLOR_SWATCHES;

  protected readonly product = signal<AdminProduct | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly stockDelta = signal(0);

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(150)],
    }),
    slug: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^([a-z0-9]+(-[a-z0-9]+)*)?$/), Validators.maxLength(120)],
    }),
    shortDescription: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(300)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(5000)],
    }),
    composition: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(1000)],
    }),
    careInstructions: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(1000)],
    }),
    categoryId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    price: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0), Validators.max(99999.99)],
    }),
    compareAtPrice: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.max(99999.99)],
    }),
    stockQuantity: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0), Validators.max(100000)],
    }),
    lowStockThreshold: new FormControl(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0), Validators.max(1000)],
    }),
    maxPerOrder: new FormControl(10, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(100)],
    }),
    active: new FormControl(true, { nonNullable: true }),
    featured: new FormControl(false, { nonNullable: true }),
    bestseller: new FormControl(false, { nonNullable: true }),
    colors: new FormControl<FlowerColor[]>([], { nonNullable: true }),
    occasionIds: new FormControl<number[]>([], { nonNullable: true }),
    images: new FormArray([imageGroup()]),
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) this.load(Number(id));
    });
  }

  get images(): FormArray<ReturnType<typeof imageGroup>> {
    return this.form.controls.images;
  }

  load(id: number): void {
    this.loading.set(true);
    this.admin.product(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.images.clear();
        (p.images.length ? p.images : [{ url: '', altText: '' }]).forEach((i) =>
          this.images.push(imageGroup(i.url, i.altText ?? '')),
        );
        this.form.patchValue({
          ...p,
          composition: p.composition ?? '',
          careInstructions: p.careInstructions ?? '',
          compareAtPrice: p.compareAtPrice ?? null,
        });
        this.form.markAsPristine();
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  toggleList<T>(control: FormControl<T[]>, value: T): void {
    const list = control.value;
    control.setValue(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
    control.markAsDirty();
  }

  addImage(): void {
    if (this.images.length < 10) this.images.push(imageGroup());
  }

  removeImage(i: number): void {
    this.images.removeAt(i);
    if (this.images.length === 0) this.images.push(imageGroup());
    this.form.markAsDirty();
  }

  moveImage(i: number, dir: -1 | 1): void {
    const j = i + dir;
    if (j < 0 || j >= this.images.length) return;
    const ctrl = this.images.at(i);
    this.images.removeAt(i);
    this.images.insert(j, ctrl);
    this.form.markAsDirty();
  }

  invalid(name: string): string | null {
    const server = this.serverErrors()[name];
    if (server) return server;
    const c = this.form.get(name);
    if (!c || !c.touched || c.valid) return null;
    if (c.hasError('required')) return 'Champ requis.';
    if (c.hasError('min')) return 'La valeur ne peut pas être négative.';
    if (c.hasError('max')) return 'Valeur trop élevée.';
    if (c.hasError('pattern'))
      return name === 'slug' ? 'Minuscules, chiffres et tirets uniquement.' : 'Format invalide.';
    return 'Valeur invalide.';
  }

  save(): void {
    this.form.markAllAsTouched();
    this.error.set('');
    this.serverErrors.set({});
    const v = this.form.getRawValue();
    if (v.compareAtPrice !== null && v.price !== null && v.compareAtPrice <= v.price) {
      this.serverErrors.set({
        compareAtPrice: 'Le prix barré doit être supérieur au prix de vente.',
      });
    }
    if (this.form.invalid || Object.keys(this.serverErrors()).length) {
      this.ui.error('Merci de corriger les champs signalés.');
      return;
    }
    const body: AdminProductRequest = {
      ...v,
      slug: v.slug || undefined,
      composition: v.composition || undefined,
      careInstructions: v.careInstructions || undefined,
      categoryId: v.categoryId!,
      price: Number(v.price),
      compareAtPrice:
        v.compareAtPrice === null || (v.compareAtPrice as unknown) === ''
          ? null
          : Number(v.compareAtPrice),
      images: v.images
        .filter((i) => i.url.trim())
        .map((i) => ({ url: i.url.trim(), altText: i.altText.trim() || undefined })),
      version: this.product()?.version,
    };
    this.saving.set(true);
    const existing = this.product();
    const req$ = existing
      ? this.admin.updateProduct(existing.id, body)
      : this.admin.createProduct(body);
    req$.subscribe({
      next: (p) => {
        this.saving.set(false);
        this.catalog.invalidate();
        this.ui.toast(
          existing ? `« ${p.name} » enregistré.` : `« ${p.name} » créé et visible en boutique.`,
        );
        if (existing) {
          this.product.set(p);
          this.form.markAsPristine();
        } else {
          void this.router.navigate(['/admin/produits', p.id]);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
        this.serverErrors.set(fieldErrors(err));
      },
    });
  }

  adjustStock(): void {
    const p = this.product();
    const delta = Math.trunc(this.stockDelta());
    if (!p || !delta) return;
    this.admin.adjustStock(p.id, delta).subscribe({
      next: (updated) => {
        this.product.set(updated);
        this.form.patchValue({ stockQuantity: updated.stockQuantity });
        this.stockDelta.set(0);
        this.ui.toast(`Stock mis à jour : ${updated.stockQuantity} en stock.`);
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }

  setDelta(event: Event): void {
    this.stockDelta.set(Number((event.target as HTMLInputElement).value) || 0);
  }
}
