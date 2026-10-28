import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../core/services/catalog.service';
import { CartService } from '../../core/services/cart.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { BouquetOptions, StemOption } from '../../models/api.models';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { QuantitySelectorComponent } from '../../shared/components/quantity-selector.component';
import { ErrorStateComponent } from '../../shared/components/states.component';

interface Bloom {
  key: string;
  stem: StemOption;
  x: number;
  y: number;
  size: number;
}

const GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

@Component({
  selector: 'app-builder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    RouterLink,
    MadPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    QuantitySelectorComponent,
    ErrorStateComponent,
  ],
  templateUrl: './builder.component.html',
  styleUrl: './builder.component.scss',
})
export class BuilderComponent {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  protected readonly options = signal<BouquetOptions | null>(null);
  protected readonly failed = signal(false);
  protected readonly step = signal(1);
  protected readonly wrappingId = signal<number | null>(null);
  protected readonly ribbonId = signal<number | null>(null);
  protected readonly stems = signal<Record<number, number>>({});
  protected readonly message = signal('');
  protected readonly quantity = signal(1);
  protected readonly adding = signal(false);

  protected readonly steps = [
    { n: 1, label: 'Emballage' },
    { n: 2, label: 'Fleurs' },
    { n: 3, label: 'Ruban' },
    { n: 4, label: 'Message' },
  ];

  protected readonly wrapping = computed(
    () => this.options()?.wrappings.find((w) => w.id === this.wrappingId()) ?? null,
  );
  protected readonly ribbon = computed(
    () => this.options()?.ribbons.find((r) => r.id === this.ribbonId()) ?? null,
  );

  protected readonly selectedStems = computed(() => {
    const opts = this.options();
    if (!opts) return [];
    return opts.stems
      .filter((s) => (this.stems()[s.id] ?? 0) > 0)
      .map((s) => ({
        stem: s,
        quantity: this.stems()[s.id],
        total: s.unitPrice * this.stems()[s.id],
      }));
  });

  protected readonly stemCount = computed(() =>
    this.selectedStems().reduce((n, s) => n + s.quantity, 0),
  );

  protected readonly unitPrice = computed(
    () =>
      (this.wrapping()?.price ?? 0) +
      (this.ribbon()?.price ?? 0) +
      this.selectedStems().reduce((sum, s) => sum + s.total, 0),
  );

  protected readonly problems = computed(() => {
    const opts = this.options();
    const issues: string[] = [];
    if (!opts) return issues;
    if (!this.wrapping()) issues.push('Choisissez un emballage.');
    if (this.stemCount() < opts.minStems)
      issues.push(`Ajoutez au moins ${opts.minStems} tiges (${this.stemCount()} pour l'instant).`);
    if (this.stemCount() > opts.maxStems)
      issues.push(`Un bouquet compte au maximum ${opts.maxStems} tiges.`);
    if (!this.ribbon()) issues.push('Choisissez un ruban.');
    return issues;
  });

  /** Phyllotaxis layout: stems spiral outward like petals in a flower head. */
  protected readonly blooms = computed<Bloom[]>(() => {
    const list: StemOption[] = [];
    for (const s of this.selectedStems()) for (let i = 0; i < s.quantity; i++) list.push(s.stem);
    const n = list.length;
    const spread = n > 1 ? 40 / Math.sqrt(n) : 0;
    const size = n <= 8 ? 26 : n <= 20 ? 20 : 16;
    return list.map((stem, i) => {
      const r = spread * Math.sqrt(i + 0.5);
      const a = i * GOLDEN_ANGLE;
      return {
        key: `${stem.id}-${i}`,
        stem,
        x: 50 + r * Math.cos(a),
        y: 50 + r * Math.sin(a),
        size,
      };
    });
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.catalog.bouquetOptions().subscribe({
      next: (opts) => {
        this.options.set(opts);
        this.wrappingId.set(opts.wrappings[0]?.id ?? null);
        this.ribbonId.set(opts.ribbons[0]?.id ?? null);
      },
      error: () => this.failed.set(true),
    });
  }

  qty(stemId: number): number {
    return this.stems()[stemId] ?? 0;
  }

  maxFor(stem: StemOption): number {
    const opts = this.options();
    const remaining = (opts?.maxStems ?? 40) - this.stemCount() + this.qty(stem.id);
    return Math.max(0, Math.min(stem.maxPerBouquet, stem.available, remaining));
  }

  setStem(stemId: number, quantity: number): void {
    this.stems.update((current) => {
      const next = { ...current };
      if (quantity <= 0) delete next[stemId];
      else next[stemId] = quantity;
      return next;
    });
  }

  goTo(step: number): void {
    this.step.set(Math.max(1, Math.min(4, step)));
    document
      .getElementById('builder-steps')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  addToCart(): void {
    if (this.problems().length) {
      this.ui.error(this.problems()[0]);
      return;
    }
    this.adding.set(true);
    this.cart
      .add({
        bouquet: {
          wrappingId: this.wrappingId()!,
          ribbonId: this.ribbonId()!,
          message: this.message().trim() || undefined,
          stems: this.selectedStems().map((s) => ({ stemId: s.stem.id, quantity: s.quantity })),
        },
        quantity: this.quantity(),
      })
      .subscribe({
        next: () => {
          this.adding.set(false);
          this.ui.toast('Votre bouquet sur mesure est dans le panier.', 'success', {
            label: 'Voir le panier',
            link: '/panier',
          });
          void this.router.navigate(['/panier']);
        },
        error: (err) => {
          this.adding.set(false);
          this.ui.error(errorMessage(err));
        },
      });
  }
}
