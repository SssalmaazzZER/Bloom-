import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UiService } from '../../core/services/ui.service';
import { IconComponent } from '../../shared/components/icon.component';

@Component({
  selector: 'app-account-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  template: `
    <div class="container">
      <header class="page-title">
        <h1>Bonjour {{ auth.user()?.firstName }}</h1>
        <p class="muted">{{ auth.user()?.email }}</p>
      </header>
      <div class="layout">
        <nav class="side" aria-label="Mon compte">
          <a
            routerLink="/compte"
            routerLinkActive="active"
            [routerLinkActiveOptions]="{ exact: true }"
            ><app-icon name="user" [size]="18" /> Profil</a
          >
          <a routerLink="/compte/commandes" routerLinkActive="active"
            ><app-icon name="box" [size]="18" /> Commandes</a
          >
          <a routerLink="/compte/adresses" routerLinkActive="active"
            ><app-icon name="pin" [size]="18" /> Adresses</a
          >
          <a routerLink="/compte/favoris" routerLinkActive="active"
            ><app-icon name="heart" [size]="18" /> Favoris</a
          >
          <a routerLink="/compte/securite" routerLinkActive="active"
            ><app-icon name="edit" [size]="18" /> Mot de passe</a
          >
          @if (auth.isAdmin()) {
            <a routerLink="/admin"><app-icon name="chart" [size]="18" /> Administration</a>
          }
          <button type="button" (click)="logout()">
            <app-icon name="logout" [size]="18" /> Se déconnecter
          </button>
        </nav>
        <section class="content"><router-outlet /></section>
      </div>
    </div>
  `,
  styles: `
    :host {
      display: block;
      padding-block-end: 96px;
    }
    .layout {
      display: grid;
      gap: 32px;
    }
    @media (min-width: 900px) {
      .layout {
        grid-template-columns: 230px 1fr;
        gap: 48px;
      }
    }
    .side {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-content: start;
    }
    @media (min-width: 900px) {
      .side {
        flex-direction: column;
        flex-wrap: nowrap;
        position: sticky;
        top: 96px;
      }
    }
    .side a,
    .side button {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      color: var(--ink);
      text-decoration: none;
      font: 500 0.95rem var(--font-body);
      border: 0;
      background: transparent;
      cursor: pointer;
      text-align: start;
      &:hover {
        background: var(--blush);
        color: var(--burgundy);
      }
      &.active {
        background: var(--burgundy);
        color: var(--white);
      }
    }
    .content {
      min-width: 0;
    }
  `,
})
export class AccountLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout().subscribe(() => {
      this.ui.toast('Vous êtes déconnecté(e). À bientôt !', 'info');
      void this.router.navigate(['/']);
    });
  }
}
