import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { IconComponent } from '../shared/components/icon.component';
import { LogoComponent } from '../shared/components/logo.component';

@Component({
  selector: 'app-admin-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent, LogoComponent],
  template: `
    <a class="skip-link" href="#admin-main">Aller au contenu</a>
    <div class="shell">
      <header class="topbar">
        <button
          type="button"
          class="icon-btn menu"
          (click)="open.set(!open())"
          aria-label="Menu d'administration"
          [attr.aria-expanded]="open()"
        >
          <app-icon name="menu" />
        </button>
        <a routerLink="/admin" class="brand"
          ><app-logo [size]="26" [light]="true" /><span>Atelier</span></a
        >
      </header>
      <nav class="side" [class.open]="open()" aria-label="Administration">
        <a routerLink="/admin" class="brand desk"
          ><app-logo [size]="28" [light]="true" /><span>Atelier</span></a
        >
        <a routerLink="/admin" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }"
          ><app-icon name="chart" [size]="19" /> Tableau de bord</a
        >
        <a routerLink="/admin/commandes" routerLinkActive="active"
          ><app-icon name="box" [size]="19" /> Commandes</a
        >
        <a routerLink="/admin/produits" routerLinkActive="active"
          ><app-icon name="tag" [size]="19" /> Produits</a
        >
        <a routerLink="/admin/inventaire" routerLinkActive="active"
          ><app-icon name="layers" [size]="19" /> Inventaire</a
        >
        <a routerLink="/admin/categories" routerLinkActive="active"
          ><app-icon name="leaf" [size]="19" /> Catégories</a
        >
        <a routerLink="/admin/clients" routerLinkActive="active"
          ><app-icon name="users" [size]="19" /> Clients</a
        >
        <span class="spacer"></span>
        <a routerLink="/"><app-icon name="arrowLeft" [size]="19" /> Voir la boutique</a>
        <button type="button" (click)="logout()">
          <app-icon name="logout" [size]="19" /> Se déconnecter
        </button>
        <p class="who">
          {{ auth.user()?.firstName }} {{ auth.user()?.lastName }}<br /><span>{{
            auth.user()?.email
          }}</span>
        </p>
      </nav>
      <main id="admin-main" class="main" tabindex="-1"><router-outlet /></main>
    </div>
  `,
  styleUrl: './admin.scss',
})
export class AdminLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly open = signal(false);

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.open.set(false));
  }

  logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/']));
  }
}
