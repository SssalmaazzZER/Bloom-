import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderDetail, OrderStatus } from '../../models/api.models';
import { STATUS_FLOW } from '../../models/labels';
import { MadPipe } from '../pipes/mad.pipe';
import { ImgSizePipe, PaymentLabelPipe, StatusLabelPipe } from '../pipes/labels.pipes';
import { ImgFallbackDirective } from '../directives/img-fallback.directive';

/** Read-only order presentation: tracking timeline, items, delivery details and totals. */
@Component({
  selector: 'app-order-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MadPipe,
    StatusLabelPipe,
    PaymentLabelPipe,
    ImgSizePipe,
    ImgFallbackDirective,
  ],
  template: `
    @let o = order();
    @if (o.status === 'CANCELLED') {
      <p class="alert">Commande annulée le {{ o.cancelledAt | date: 'd MMMM y à HH:mm' }}.</p>
    } @else {
      <ol class="timeline" aria-label="Suivi de la commande">
        @for (s of flow; track s; let i = $index) {
          <li
            [class.done]="i <= currentIndex()"
            [class.current]="i === currentIndex()"
            [attr.aria-current]="i === currentIndex() ? 'step' : null"
          >
            <span class="dot" aria-hidden="true"></span>
            <span class="label">{{ s | statusLabel }}</span>
            @if (eventAt(s); as at) {
              <span class="at">{{ at | date: 'd MMM, HH:mm' }}</span>
            }
          </li>
        }
      </ol>
    }

    <div class="grid">
      <section class="block" aria-labelledby="items-{{ o.id }}">
        <h3 id="items-{{ o.id }}">Articles</h3>
        <ul class="items">
          @for (item of o.items; track item.id) {
            <li>
              <img
                appImgFallback
                [src]="item.imageUrl | imgSize: 160"
                alt=""
                width="64"
                height="80"
              />
              <div>
                <strong>
                  @if (item.productSlug && linkProducts()) {
                    <a [routerLink]="['/produit', item.productSlug]">{{ item.name }}</a>
                  } @else {
                    {{ item.name }}
                  }
                </strong>
                <span class="muted">{{ item.quantity }} × {{ item.unitPrice | mad }}</span>
                @if (item.bouquet; as b) {
                  <span class="muted small">
                    @for (s of b.stems; track s.name; let last = $last) {
                      {{ s.quantity }} {{ s.name }}{{ last ? '' : ', ' }}
                    }
                    <br />{{ b.wrappingName }}, ruban {{ b.ribbonName }}
                    @if (b.message) {
                      <br /><em>« {{ b.message }} »</em>
                    }
                  </span>
                }
              </div>
              <span class="price">{{ item.lineTotal | mad }}</span>
            </li>
          }
        </ul>
        <dl class="totals">
          <div>
            <dt>Sous-total</dt>
            <dd>{{ o.subtotal | mad }}</dd>
          </div>
          <div>
            <dt>Livraison</dt>
            <dd>{{ o.deliveryFee > 0 ? (o.deliveryFee | mad) : 'Offerte' }}</dd>
          </div>
          @if (o.giftWrappingFee > 0) {
            <div>
              <dt>Emballage cadeau</dt>
              <dd>{{ o.giftWrappingFee | mad }}</dd>
            </div>
          }
          @if (o.discount > 0) {
            <div>
              <dt>Remise</dt>
              <dd>− {{ o.discount | mad }}</dd>
            </div>
          }
          <div class="total">
            <dt>Total</dt>
            <dd>{{ o.total | mad }}</dd>
          </div>
        </dl>
      </section>

      <section class="block" aria-labelledby="delivery-{{ o.id }}">
        <h3 id="delivery-{{ o.id }}">Livraison</h3>
        <p>
          <strong>{{ o.deliveryDate | date: 'EEEE d MMMM y' }}</strong
          ><br />
          {{ o.deliverySlotLabel }}
        </p>
        <p>
          {{ o.recipientName }}<br />
          {{ o.addressLine1 }}<br />
          @if (o.addressLine2) {
            {{ o.addressLine2 }}<br />
          }
          {{ o.postalCode }} {{ o.city }}<br />
          {{ o.recipientPhone }}
        </p>
        @if (o.deliveryInstructions) {
          <p class="muted">Instructions : {{ o.deliveryInstructions }}</p>
        }
        @if (o.giftMessage || o.giftWrapping) {
          <h3>Cadeau</h3>
          @if (o.giftWrapping) {
            <p>Emballage cadeau premium</p>
          }
          @if (o.giftMessage) {
            <p class="card-msg">« {{ o.giftMessage }} »</p>
          }
        }
        <h3>Paiement</h3>
        <p>
          Paiement à la livraison<br /><span class="muted">{{
            o.paymentStatus | paymentLabel
          }}</span>
        </p>
        @if (showCustomer()) {
          <h3>Client</h3>
          <p>
            {{ o.customerName }}<br /><a [href]="'mailto:' + o.customerEmail">{{
              o.customerEmail
            }}</a
            ><br />{{ o.customerPhone }}
          </p>
        }
      </section>
    </div>
  `,
  styles: `
    .timeline {
      list-style: none;
      padding: 0;
      margin: 0 0 28px;
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 4px;
      li {
        position: relative;
        display: grid;
        align-content: start;
        justify-items: center;
        text-align: center;
        gap: 6px;
        font-size: 0.86rem;
        color: var(--ink-soft);
      }
      li::before {
        content: '';
        position: absolute;
        top: 9px;
        inset-inline-start: -50%;
        width: 100%;
        height: 2px;
        background: var(--line);
        z-index: 0;
      }
      li:first-child::before {
        display: none;
      }
      li.done::before {
        background: var(--sage);
      }
      .dot {
        position: relative;
        z-index: 1;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background: var(--white);
        border: 2px solid var(--line);
      }
      li.done .dot {
        background: var(--sage);
        border-color: var(--sage);
      }
      li.current .dot {
        background: var(--burgundy);
        border-color: var(--burgundy);
        box-shadow: 0 0 0 4px var(--blush);
      }
      li.current {
        color: var(--burgundy);
        font-weight: 600;
      }
      .at {
        font-size: 0.78rem;
      }
    }
    @media (max-width: 599px) {
      .timeline {
        grid-template-columns: 1fr;
        gap: 12px;
        li {
          grid-template-columns: 20px auto 1fr;
          justify-items: start;
          text-align: start;
          align-items: center;
          gap: 10px;
        }
        li::before {
          display: none;
        }
      }
    }
    .grid {
      display: grid;
      gap: 24px;
    }
    @media (min-width: 900px) {
      .grid {
        grid-template-columns: 1.4fr 1fr;
      }
    }
    .block {
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      padding: 22px;
      h3 {
        font-size: 1.15rem;
        margin-block: 0 12px;
      }
      h3:not(:first-child) {
        margin-block-start: 20px;
      }
      p {
        margin-block-end: 10px;
      }
    }
    .items {
      list-style: none;
      padding: 0;
      margin: 0;
      li {
        display: grid;
        grid-template-columns: 52px 1fr auto;
        gap: 14px;
        align-items: start;
        padding-block: 12px;
        border-block-end: 1px solid var(--line);
      }
      img {
        width: 52px;
        height: 65px;
        object-fit: cover;
        border-radius: 999px 999px 6px 6px;
        background: var(--blush);
      }
      div {
        display: grid;
        gap: 2px;
        font-size: 0.94rem;
      }
      a {
        color: inherit;
      }
    }
    .small {
      font-size: 0.85rem;
      line-height: 1.45;
    }
    .totals {
      margin: 14px 0 0;
      div {
        display: flex;
        justify-content: space-between;
        padding-block: 5px;
      }
      dt {
        color: var(--ink-soft);
      }
      dd {
        margin: 0;
        font-variant-numeric: tabular-nums;
      }
      .total {
        font-weight: 700;
        font-size: 1.1rem;
        border-block-start: 1px solid var(--line);
        padding-block-start: 10px;
        margin-block-start: 6px;
      }
      .total dd {
        color: var(--burgundy);
      }
    }
    .card-msg {
      font-family: var(--font-display);
      font-style: italic;
      color: var(--burgundy);
      background: var(--cream);
      padding: 10px 14px;
      border-radius: var(--radius-sm);
    }
  `,
})
export class OrderViewComponent {
  readonly order = input.required<OrderDetail>();
  readonly showCustomer = input(false);
  readonly linkProducts = input(true);

  protected readonly flow = STATUS_FLOW;
  protected readonly currentIndex = computed(() => STATUS_FLOW.indexOf(this.order().status));

  eventAt(status: OrderStatus): string | null {
    const events = this.order().history.filter((h) => h.status === status);
    return events.length ? events[events.length - 1].at : null;
  }
}
