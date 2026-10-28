import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { phoneValidator } from '../../shared/validators/validators';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/services/auth.service';
import { AccountService } from '../../core/services/account.service';
import { CartService } from '../../core/services/cart.service';
import { CatalogService } from '../../core/services/catalog.service';
import { OrderService } from '../../core/services/order.service';
import { UiService } from '../../core/services/ui.service';
import { apiError, fieldErrors } from '../../core/errors';
import { Address, CartView, DeliveryOptions, DeliverySlot } from '../../models/api.models';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { IconComponent } from '../../shared/components/icon.component';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';
import { catchError, of } from 'rxjs';

function newIdempotencyKey(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `chk-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

@Component({
  selector: 'app-checkout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    MadPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    IconComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent {
  private readonly auth = inject(AuthService);
  private readonly account = inject(AccountService);
  private readonly cartService = inject(CartService);
  private readonly catalog = inject(CatalogService);
  private readonly orders = inject(OrderService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  /** One key per checkout visit: double clicks or retries never create two orders. */
  private idempotencyKey = newIdempotencyKey();

  protected readonly delivery = signal<DeliveryOptions | null>(null);
  protected readonly summary = signal<CartView | null>(null);
  protected readonly loadFailed = signal(false);
  protected readonly submitting = signal(false);
  protected readonly serverError = signal<{ code?: string; message: string } | null>(null);
  protected readonly serverFieldErrors = signal<Record<string, string>>({});
  protected readonly addresses = toSignal(
    this.account.addresses().pipe(catchError(() => of([] as Address[]))),
    { initialValue: [] as Address[] },
  );

  protected readonly form = new FormGroup({
    customerName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    customerEmail: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)],
    }),
    customerPhone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, phoneValidator],
    }),
    forSomeoneElse: new FormControl(false, { nonNullable: true }),
    recipientName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(160)],
    }),
    recipientPhone: new FormControl('', { nonNullable: true, validators: [phoneValidator] }),
    addressLine1: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(200)],
    }),
    addressLine2: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(200)],
    }),
    city: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    postalCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(\d{5})?$/)],
    }),
    deliveryInstructions: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
    deliveryDate: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    deliverySlot: new FormControl<DeliverySlot | ''>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    giftWrapping: new FormControl(false, { nonNullable: true }),
    giftMessage: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(300)],
    }),
  });

  private readonly selectedDate = toSignal(this.form.controls.deliveryDate.valueChanges, {
    initialValue: '',
  });
  protected readonly forSomeoneElse = toSignal(this.form.controls.forSomeoneElse.valueChanges, {
    initialValue: false,
  });
  protected readonly giftMessageLength = toSignal(this.form.controls.giftMessage.valueChanges, {
    initialValue: '',
  });

  /** Same-day slots are restricted by the server's cut-off rules. */
  protected readonly availableSlots = computed(() => {
    const d = this.delivery();
    if (!d) return [];
    const date = this.selectedDate();
    return d.slots.filter((s) => date !== d.earliestDate || d.earliestDateSlots.includes(s.code));
  });

  constructor() {
    const user = this.auth.user();
    if (user) {
      this.form.patchValue({
        customerName: `${user.firstName} ${user.lastName}`,
        customerEmail: user.email,
        customerPhone: user.phone ?? '',
      });
    }
    this.load();
    this.form.controls.giftWrapping.valueChanges.subscribe((g) => this.refreshSummary(g));
    this.form.controls.deliveryDate.valueChanges.subscribe(() => {
      const slot = this.form.controls.deliverySlot.value;
      if (slot && !this.availableSlots().some((s) => s.code === slot)) {
        this.form.controls.deliverySlot.setValue('');
      }
    });
  }

  load(): void {
    this.loadFailed.set(false);
    this.catalog.deliveryOptions().subscribe({
      next: (d) => {
        this.delivery.set(d);
        if (!this.form.controls.deliveryDate.value) {
          const tomorrow = this.addDays(d.earliestDate, d.earliestDateSlots.length ? 0 : 1);
          this.form.controls.deliveryDate.setValue(tomorrow);
        }
      },
      error: () => this.loadFailed.set(true),
    });
    this.refreshSummary(this.form.controls.giftWrapping.value);
  }

  refreshSummary(giftWrapping: boolean): void {
    this.cartService.preview(giftWrapping).subscribe({
      next: (c) => this.summary.set(c),
      error: () => this.loadFailed.set(true),
    });
  }

  useAddress(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    const a = this.addresses().find((x) => x.id === id);
    if (!a) return;
    const user = this.auth.user();
    const isSelf = user && `${user.firstName} ${user.lastName}` === a.recipientName;
    this.form.patchValue({
      addressLine1: a.line1,
      addressLine2: a.line2 ?? '',
      city: this.delivery()?.cities.find((c) => c.toLowerCase() === a.city.toLowerCase()) ?? a.city,
      postalCode: a.postalCode ?? '',
      forSomeoneElse: !isSelf,
      recipientName: isSelf ? '' : a.recipientName,
      recipientPhone: isSelf ? '' : a.phone,
    });
  }

  error(name: keyof typeof this.form.controls): string | null {
    const c = this.form.controls[name];
    const server = this.serverFieldErrors()[name];
    if (server) return server;
    if (!c.touched || c.valid) return null;
    if (c.hasError('required')) return 'Ce champ est requis.';
    if (c.hasError('email')) return 'Adresse e-mail invalide.';
    if (c.hasError('phone')) return 'Numéro invalide (ex. 06 12 34 56 78).';
    if (c.hasError('pattern')) return 'Le code postal doit contenir 5 chiffres.';
    if (c.hasError('maxlength')) return 'Texte trop long.';
    return 'Valeur invalide.';
  }

  submit(): void {
    this.serverError.set(null);
    this.serverFieldErrors.set({});
    this.form.markAllAsTouched();
    if (this.forSomeoneElse() && !this.form.controls.recipientName.value.trim()) {
      this.serverFieldErrors.set({ recipientName: 'Indiquez le nom du destinataire.' });
    }
    if (this.form.invalid || Object.keys(this.serverFieldErrors()).length) {
      this.focusFirstError();
      return;
    }
    const v = this.form.getRawValue();
    const summary = this.summary();
    this.submitting.set(true);
    this.orders
      .checkout({
        customerName: v.customerName.trim(),
        customerEmail: v.customerEmail.trim(),
        customerPhone: v.customerPhone,
        recipientName: v.forSomeoneElse ? v.recipientName.trim() : undefined,
        recipientPhone: v.forSomeoneElse && v.recipientPhone ? v.recipientPhone : undefined,
        addressLine1: v.addressLine1.trim(),
        addressLine2: v.addressLine2.trim() || undefined,
        city: v.city,
        postalCode: v.postalCode || undefined,
        deliveryInstructions: v.deliveryInstructions.trim() || undefined,
        giftMessage: v.giftMessage.trim() || undefined,
        giftWrapping: v.giftWrapping,
        deliveryDate: v.deliveryDate,
        deliverySlot: v.deliverySlot as DeliverySlot,
        paymentMethod: 'CASH_ON_DELIVERY',
        idempotencyKey: this.idempotencyKey,
        expectedTotal: summary?.total,
      })
      .subscribe({
        next: (order) => {
          this.cartService.markOrdered();
          void this.router.navigate(['/commande/confirmation', order.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          const e = apiError(err);
          // Refused by the server (4xx): the next attempt is a new order. On network errors the key is
          // kept, so a retry returns the original order if it had in fact been created.
          if (err instanceof HttpErrorResponse && err.status >= 400 && err.status < 500) {
            this.idempotencyKey = newIdempotencyKey();
          }
          this.serverError.set({
            code: e.code,
            message: e.message ?? "La commande n'a pas pu être validée.",
          });
          this.serverFieldErrors.set(fieldErrors(err));
          if (
            e.code === 'PRICE_CHANGED' ||
            e.code === 'CART_NEEDS_REVIEW' ||
            e.code === 'INSUFFICIENT_STOCK'
          ) {
            this.refreshSummary(v.giftWrapping);
            this.cartService.refresh().subscribe({ error: () => undefined });
          }
          if (e.code?.startsWith('DELIVERY_')) {
            this.catalog
              .deliveryOptions()
              .subscribe({ next: (d) => this.delivery.set(d), error: () => undefined });
          }
          window.scrollTo({ top: 0, behavior: 'smooth' });
        },
      });
  }

  private focusFirstError(): void {
    setTimeout(() => {
      const el = document.querySelector<HTMLElement>(
        'form .ng-invalid:not(form), [aria-invalid="true"]',
      );
      el?.focus();
    });
    this.ui.error('Merci de compléter les champs signalés.');
  }

  private addDays(isoDate: string, days: number): string {
    const [y, m, d] = isoDate.split('-').map(Number);
    const date = new Date(Date.UTC(y, m - 1, d + days));
    return date.toISOString().slice(0, 10);
  }
}
