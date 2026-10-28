import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../core/services/admin.service';
import { UiService } from '../core/services/ui.service';
import { errorMessage } from '../core/errors';
import { OrderDetail, OrderStatus } from '../models/api.models';
import { OrderViewComponent } from '../shared/components/order-view.component';
import { StatusLabelPipe } from '../shared/pipes/labels.pipes';
import { EmptyStateComponent } from '../shared/components/states.component';

@Component({
  selector: 'app-admin-order',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    FormsModule,
    OrderViewComponent,
    StatusLabelPipe,
    EmptyStateComponent,
  ],
  template: `
    <a routerLink="/admin/commandes" class="back">Toutes les commandes</a>
    @if (order(); as o) {
      <div class="head">
        <div>
          <h1>{{ o.reference }}</h1>
          <p>Passée le {{ o.createdAt | date: "d MMMM y 'à' HH:mm" }} par {{ o.customerName }}</p>
        </div>
        <span [class]="'badge status-' + o.status">{{ o.status | statusLabel }}</span>
      </div>

      <section class="card actions" aria-labelledby="status-title">
        <h2 id="status-title">Faire avancer la commande</h2>
        @if (o.allowedTransitions.length) {
          <div class="field">
            <label for="note">Note interne (facultatif, visible dans l'historique)</label>
            <input
              id="note"
              class="input"
              [(ngModel)]="note"
              maxlength="300"
              placeholder="Ex. : appelé la destinataire, livraison à 18h"
            />
          </div>
          <div class="buttons">
            @for (t of o.allowedTransitions; track t) {
              <button
                type="button"
                class="btn btn-sm"
                [class.btn-danger]="t === 'CANCELLED'"
                [disabled]="saving()"
                (click)="move(o, t)"
              >
                {{
                  t === 'CANCELLED'
                    ? 'Annuler la commande'
                    : 'Passer à « ' + (t | statusLabel) + ' »'
                }}
              </button>
            }
          </div>
        } @else {
          <p class="muted">
            Cette commande est {{ o.status === 'DELIVERED' ? 'livrée' : 'annulée' }} : son statut
            est définitif.
          </p>
        }
        @if (o.history.length) {
          <h3>Historique</h3>
          <ol class="history">
            @for (h of o.history; track $index) {
              <li>
                <strong>{{ h.status | statusLabel }}</strong
                >{{ ' ' }}<span class="muted">{{ h.at | date: 'd MMM y, HH:mm' }}</span>
                @if (h.note) {
                  <br /><span>{{ h.note }}</span>
                }
              </li>
            }
          </ol>
        }
      </section>

      <app-order-view [order]="o" [showCustomer]="true" [linkProducts]="false" />
    } @else if (missing()) {
      <app-empty-state title="Commande introuvable" message="Cette commande n'existe pas.">
        <a class="btn" routerLink="/admin/commandes">Retour aux commandes</a>
      </app-empty-state>
    } @else {
      <div class="skeleton" style="height: 400px"></div>
    }
  `,
  styleUrls: ['./admin-shared.scss'],
  styles: `
    .back {
      display: inline-block;
      margin-block-end: 12px;
      font-size: 0.9rem;
    }
    .actions {
      margin-block-end: 22px;
      h2 {
        font-size: 1.25rem;
      }
      h3 {
        font-size: 1.05rem;
        margin-block-start: 22px;
      }
    }
    .buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
    .history {
      padding-inline-start: 20px;
      margin: 0;
      display: grid;
      gap: 8px;
      font-size: 0.92rem;
    }
  `,
})
export class AdminOrderComponent {
  readonly id = input.required<string>();
  private readonly admin = inject(AdminService);
  private readonly ui = inject(UiService);
  protected readonly order = signal<OrderDetail | null>(null);
  protected readonly missing = signal(false);
  protected readonly saving = signal(false);
  protected note = '';

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.admin
        .order(id)
        .subscribe({ next: (o) => this.order.set(o), error: () => this.missing.set(true) });
    });
  }

  async move(order: OrderDetail, target: OrderStatus): Promise<void> {
    if (target === 'CANCELLED') {
      const ok = await this.ui.confirm({
        title: 'Annuler cette commande ?',
        message: `La commande ${order.reference} sera annulée et son stock remis en vente. Ce statut est définitif.`,
        confirmLabel: 'Annuler la commande',
        cancelLabel: 'Ne pas annuler',
        danger: true,
      });
      if (!ok) return;
    }
    this.saving.set(true);
    this.admin.updateStatus(order.id, target, this.note.trim() || undefined).subscribe({
      next: (o) => {
        this.saving.set(false);
        this.note = '';
        this.order.set(o);
        this.ui.toast(`Commande ${o.reference} : statut mis à jour.`);
      },
      error: (err) => {
        this.saving.set(false);
        this.ui.error(errorMessage(err));
      },
    });
  }
}
