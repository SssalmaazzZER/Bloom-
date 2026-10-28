import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="state" role="status">
      <span class="mark"><app-icon [name]="icon()" [size]="30" /></span>
      <h2>{{ title() }}</h2>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      <div class="actions"><ng-content /></div>
    </div>
  `,
  styleUrl: './states.scss',
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly message = input<string>();
  readonly icon = input('leaf');
}

@Component({
  selector: 'app-error-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="state error" role="alert">
      <span class="mark"><app-icon name="alert" [size]="30" /></span>
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      <div class="actions">
        <button type="button" class="btn btn-ghost" (click)="retry.emit()">Réessayer</button>
        <ng-content />
      </div>
    </div>
  `,
  styleUrl: './states.scss',
})
export class ErrorStateComponent {
  readonly title = input('Impossible de charger cette page');
  readonly message = input(
    'Le serveur ne répond pas pour le moment. Vérifiez votre connexion puis réessayez.',
  );
  readonly retry = output<void>();
}

@Component({
  selector: 'app-product-grid-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="grid" aria-busy="true" aria-label="Chargement des produits">
      @for (i of items(); track i) {
        <div class="sk">
          <div class="skeleton img"></div>
          <div class="skeleton line w40"></div>
          <div class="skeleton line w80"></div>
          <div class="skeleton line w60"></div>
        </div>
      }
    </div>
  `,
  styles: `
    .grid {
      display: grid;
      gap: 24px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 240px), 1fr));
    }
    .sk {
      border: 1px solid var(--line);
      border-radius: var(--radius-lg);
      overflow: hidden;
      background: var(--white);
      padding-block-end: 18px;
    }
    .img {
      aspect-ratio: 4 / 5;
      border-radius: 0;
      margin-block-end: 16px;
    }
    .line {
      height: 14px;
      margin: 10px 18px 0;
    }
    .w40 {
      width: 40%;
    }
    .w60 {
      width: 60%;
    }
    .w80 {
      width: 80%;
    }
  `,
})
export class ProductGridSkeletonComponent {
  readonly count = input(8);
  protected items(): number[] {
    return Array.from({ length: this.count() }, (_, i) => i);
  }
}
