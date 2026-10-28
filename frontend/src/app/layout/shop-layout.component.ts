import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FooterComponent } from './footer.component';
import { HeaderComponent } from './header.component';

@Component({
  selector: 'app-shop-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <a class="skip-link" href="#main">Aller au contenu</a>
    <app-header />
    <main id="main" tabindex="-1">
      <router-outlet />
    </main>
    <app-footer />
  `,
  styles: `
    main {
      display: block;
      min-height: 60vh;
      outline: none;
    }
  `,
})
export class ShopLayoutComponent {}
