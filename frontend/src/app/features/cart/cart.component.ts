import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { CartLine } from '../../models/api.models';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { ImgSizePipe } from '../../shared/pipes/labels.pipes';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { QuantitySelectorComponent } from '../../shared/components/quantity-selector.component';
import { IconComponent } from '../../shared/components/icon.component';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-cart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MadPipe,
    ImgSizePipe,
    ImgFallbackDirective,
    QuantitySelectorComponent,
    IconComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent {
  protected readonly cart = inject(CartService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  protected readonly failed = signal(false);
  protected readonly loading = signal(true);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.cart.refresh().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });
  }

  issueText(line: CartLine): string | null {
    switch (line.issue) {
      case 'UNAVAILABLE':
        return "Cet article n'est plus proposé. Retirez-le pour continuer.";
      case 'OUT_OF_STOCK':
        return 'Épuisé depuis votre ajout. Retirez-le pour continuer.';
      case 'QUANTITY_EXCEEDS_STOCK':
        return `Seulement ${line.maxQuantity} disponible(s) : ajustez la quantité.`;
      default:
        return null;
    }
  }

  setQuantity(line: CartLine, quantity: number): void {
    if (quantity === line.quantity) return;
    this.cart
      .update(line.id, quantity)
      .subscribe({ error: (err) => this.ui.error(errorMessage(err)) });
  }

  fix(line: CartLine): void {
    this.setQuantity(line, Math.max(1, line.maxQuantity));
  }

  async remove(line: CartLine): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Retirer cet article ?',
      message: `« ${line.name} » sera retiré de votre panier.`,
      confirmLabel: 'Retirer',
      danger: true,
    });
    if (!ok) return;
    this.cart.remove(line.id).subscribe({
      next: () => this.ui.toast(`« ${line.name} » retiré du panier.`, 'info'),
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }

  checkout(): void {
    if (this.auth.isLoggedIn()) {
      void this.router.navigate(['/commande']);
    } else {
      void this.router.navigate(['/connexion'], {
        queryParams: { returnUrl: '/commande', panier: 1 },
      });
    }
  }
}
