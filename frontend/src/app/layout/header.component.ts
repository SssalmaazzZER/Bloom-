import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../core/services/auth.service';
import { CartService } from '../core/services/cart.service';
import { IconComponent } from '../shared/components/icon.component';
import { LogoComponent } from '../shared/components/logo.component';

interface NavLink {
  label: string;
  link: string;
  query?: Record<string, string | string[]>;
}

@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent, LogoComponent],
  template: `
    <div class="announce" role="region" aria-label="Annonce">
      <p>
        Un peu de beauté, livrée avec amour — livraison offerte dès 500 MAD à Rabat, Kénitra &amp;
        Casablanca.
      </p>
    </div>
    <header class="bar" [class.scrolled]="scrolled()">
      <div class="container inner">
        <button
          type="button"
          class="icon-btn menu-toggle"
          (click)="openMenu()"
          aria-label="Ouvrir le menu"
          [attr.aria-expanded]="menuOpen()"
          aria-controls="mobile-menu"
        >
          <app-icon name="menu" />
        </button>

        <a routerLink="/" class="brand" aria-label="Bloomé, retour à l'accueil"
          ><app-logo [size]="32"
        /></a>

        <nav class="links" aria-label="Navigation principale">
          @for (l of links; track l.label) {
            <a
              [routerLink]="l.link"
              [queryParams]="l.query ?? null"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: true }"
              >{{ l.label }}</a
            >
          }
        </nav>

        <div class="tools">
          <button
            type="button"
            class="icon-btn"
            (click)="toggleSearch()"
            aria-label="Rechercher"
            [attr.aria-expanded]="searchOpen()"
            aria-controls="site-search"
          >
            <app-icon name="search" />
          </button>
          <a
            class="icon-btn"
            [routerLink]="
              auth.isLoggedIn() ? (auth.isAdmin() ? '/admin' : '/compte') : '/connexion'
            "
            [attr.aria-label]="auth.isLoggedIn() ? 'Mon compte' : 'Se connecter'"
          >
            <app-icon name="user" />
          </a>
          <a
            class="icon-btn cart"
            routerLink="/panier"
            [attr.aria-label]="'Panier, ' + cart.count() + ' article(s)'"
          >
            <app-icon name="bag" />
            @if (cart.count() > 0) {
              <span class="count" aria-hidden="true">{{ cart.count() }}</span>
            }
          </a>
        </div>
      </div>

      @if (searchOpen()) {
        <form id="site-search" class="search container" role="search" (submit)="search($event)">
          <label for="q" class="visually-hidden"
            >Rechercher une fleur, un bouquet, une plante</label
          >
          <app-icon name="search" />
          <input
            #searchInput
            id="q"
            name="q"
            type="search"
            placeholder="Pivoines, roses rouges, orchidée…"
            autocomplete="off"
            maxlength="100"
          />
          <button type="submit" class="btn btn-sm">Rechercher</button>
        </form>
      }
    </header>

    @if (menuOpen()) {
      <div class="scrim" (click)="menuOpen.set(false)" aria-hidden="true"></div>
      <nav id="mobile-menu" class="drawer" aria-label="Menu">
        <div class="drawer-head">
          <app-logo [size]="28" />
          <button
            type="button"
            class="icon-btn"
            (click)="menuOpen.set(false)"
            aria-label="Fermer le menu"
          >
            <app-icon name="close" />
          </button>
        </div>
        @for (l of links; track l.label) {
          <a [routerLink]="l.link" [queryParams]="l.query ?? null" (click)="menuOpen.set(false)">{{
            l.label
          }}</a>
        }
        <hr />
        <a
          [routerLink]="auth.isLoggedIn() ? '/compte' : '/connexion'"
          (click)="menuOpen.set(false)"
        >
          {{ auth.isLoggedIn() ? 'Mon compte' : 'Se connecter' }}
        </a>
        @if (auth.isAdmin()) {
          <a routerLink="/admin" (click)="menuOpen.set(false)">Administration</a>
        }
      </nav>
    }
  `,
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  protected readonly menuOpen = signal(false);
  protected readonly searchOpen = signal(false);
  protected readonly scrolled = signal(false);

  protected readonly links: NavLink[] = [
    { label: 'Boutique', link: '/boutique' },
    { label: 'Bouquets', link: '/boutique', query: { categorie: 'bouquets' } },
    { label: 'Fleurs', link: '/boutique', query: { categorie: ['roses', 'saison'] } },
    { label: 'Plantes', link: '/boutique', query: { categorie: 'plantes' } },
    { label: 'Occasions', link: '/occasions' },
    { label: 'Créer mon bouquet', link: '/creer-mon-bouquet' },
    { label: 'À propos', link: '/a-propos' },
  ];

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.menuOpen.set(false);
        this.searchOpen.set(false);
      });
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.menuOpen.set(false);
    this.searchOpen.set(false);
  }

  /** Opens the mobile drawer and moves keyboard focus into it. */
  openMenu(): void {
    this.menuOpen.set(true);
    setTimeout(() => document.querySelector<HTMLElement>('#mobile-menu button')?.focus());
  }

  toggleSearch(): void {
    this.searchOpen.update((v) => !v);
    if (this.searchOpen()) {
      setTimeout(() => this.searchInput()?.nativeElement.focus());
    }
  }

  search(event: Event): void {
    event.preventDefault();
    const q = this.searchInput()?.nativeElement.value.trim() ?? '';
    void this.router.navigate(['/boutique'], { queryParams: q ? { q } : {} });
  }
}
