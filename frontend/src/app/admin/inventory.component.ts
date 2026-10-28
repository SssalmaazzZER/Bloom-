import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService } from '../core/services/admin.service';
import { UiService } from '../core/services/ui.service';
import { errorMessage } from '../core/errors';
import { Inventory } from '../models/api.models';
import { MadPipe } from '../shared/pipes/mad.pipe';
import { ColorLabelPipe } from '../shared/pipes/labels.pipes';
import { ErrorStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-inventory',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MadPipe, ColorLabelPipe, ErrorStateComponent],
  template: `
    <div class="head">
      <div>
        <h1>Inventaire</h1>
        <p>{{ lowCount() }} article(s) en stock bas ou épuisé(s).</p>
      </div>
      <label class="check"
        ><input type="checkbox" [checked]="onlyLow()" (change)="onlyLow.set(!onlyLow())" />
        <span>Afficher uniquement les alertes</span></label
      >
    </div>
    @if (failed()) {
      <app-error-state (retry)="load()" />
    } @else if (data(); as d) {
      <h2>Produits</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Produit</th>
              <th class="num">Seuil</th>
              <th class="num">Stock</th>
              <th>Ajuster</th>
            </tr>
          </thead>
          <tbody>
            @for (p of d.products; track p.id) {
              @if (!onlyLow() || p.lowStock) {
                <tr>
                  <td>
                    <a [routerLink]="['/admin/produits', p.id]">{{ p.name }}</a>
                    @if (!p.active) {
                      <span class="badge">Désactivé</span>
                    }
                  </td>
                  <td class="num">{{ p.lowStockThreshold }}</td>
                  <td class="num">
                    <span
                      class="badge"
                      [class.danger]="p.stockQuantity === 0"
                      [class.warn]="p.lowStock && p.stockQuantity > 0"
                      [class.sage]="!p.lowStock"
                      >{{ p.stockQuantity }}</span
                    >
                  </td>
                  <td>
                    <form class="adjust" (submit)="adjustProduct($event, p.id, p.name)">
                      <label class="visually-hidden" [for]="'p-' + p.id"
                        >Variation de stock pour {{ p.name }}</label
                      >
                      <input
                        class="input"
                        type="number"
                        step="1"
                        [id]="'p-' + p.id"
                        name="delta"
                        placeholder="+/-"
                      />
                      <button type="submit" class="btn btn-soft btn-sm">OK</button>
                    </form>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <h2>Fleurs à l'unité (atelier « Créer mon bouquet »)</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Fleur</th>
              <th>Couleur</th>
              <th class="num">Prix / tige</th>
              <th class="num">Stock</th>
              <th>Ajuster</th>
            </tr>
          </thead>
          <tbody>
            @for (s of d.stems; track s.id) {
              @if (!onlyLow() || s.lowStock) {
                <tr>
                  <td>{{ s.name }}</td>
                  <td>{{ s.color | colorLabel }}</td>
                  <td class="num">{{ s.unitPrice | mad }}</td>
                  <td class="num">
                    <span
                      class="badge"
                      [class.danger]="s.stockQuantity === 0"
                      [class.warn]="s.lowStock && s.stockQuantity > 0"
                      [class.sage]="!s.lowStock"
                      >{{ s.stockQuantity }}</span
                    >
                  </td>
                  <td>
                    <form class="adjust" (submit)="adjustStem($event, s.id, s.name)">
                      <label class="visually-hidden" [for]="'s-' + s.id"
                        >Variation de stock pour {{ s.name }}</label
                      >
                      <input
                        class="input"
                        type="number"
                        step="1"
                        [id]="'s-' + s.id"
                        name="delta"
                        placeholder="+/-"
                      />
                      <button type="submit" class="btn btn-soft btn-sm">OK</button>
                    </form>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    } @else {
      <div class="skeleton" style="height: 400px"></div>
    }
  `,
  styleUrls: ['./admin-shared.scss'],
  styles: `
    h2 {
      font-size: 1.3rem;
      margin-block: 28px 12px;
    }
    .adjust {
      display: flex;
      gap: 6px;
      .input {
        width: 90px;
        min-height: 36px;
        padding: 6px 10px;
      }
    }
  `,
})
export class InventoryComponent {
  private readonly admin = inject(AdminService);
  private readonly ui = inject(UiService);
  protected readonly data = signal<Inventory | null>(null);
  protected readonly failed = signal(false);
  protected readonly onlyLow = signal(false);
  protected readonly lowCount = computed(() => {
    const d = this.data();
    return d
      ? d.products.filter((p) => p.lowStock).length + d.stems.filter((s) => s.lowStock).length
      : 0;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.admin
      .inventory()
      .subscribe({ next: (d) => this.data.set(d), error: () => this.failed.set(true) });
  }

  private readDelta(event: Event): { delta: number; form: HTMLFormElement } | null {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const delta = Math.trunc(Number((form.elements.namedItem('delta') as HTMLInputElement).value));
    return delta ? { delta, form } : null;
  }

  adjustProduct(event: Event, id: number, name: string): void {
    const input = this.readDelta(event);
    if (!input) return;
    this.admin.adjustStock(id, input.delta).subscribe({
      next: (p) => {
        input.form.reset();
        this.ui.toast(`${name} : ${p.stockQuantity} en stock.`);
        this.load();
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }

  adjustStem(event: Event, id: number, name: string): void {
    const input = this.readDelta(event);
    if (!input) return;
    this.admin.adjustStemStock(id, input.delta).subscribe({
      next: (s) => {
        input.form.reset();
        this.ui.toast(`${name} : ${s.stockQuantity} tiges en stock.`);
        this.load();
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }
}
