import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../core/services/catalog.service';
import { errorMessage } from '../core/errors';
import { IconComponent } from '../shared/components/icon.component';
import { LogoComponent } from '../shared/components/logo.component';

@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, LogoComponent, ReactiveFormsModule],
  template: `
    <section class="newsletter" aria-labelledby="nl-title">
      <div class="container nl-inner">
        <div>
          <h2 id="nl-title">La lettre des saisons</h2>
          <p>
            Une fois par mois : les fleurs qui arrivent, nos conseils d'entretien et les collections
            en avant-première.
          </p>
        </div>
        <form (submit)="subscribe($event)" novalidate>
          @if (done()) {
            <p class="ok" role="status"><app-icon name="check" /> {{ done() }}</p>
          } @else {
            <label for="nl-email" class="visually-hidden">Votre adresse e-mail</label>
            <div class="nl-row">
              <input
                id="nl-email"
                class="input"
                type="email"
                [formControl]="email"
                placeholder="votre@email.ma"
                autocomplete="email"
                [attr.aria-invalid]="showError() ? true : null"
                [attr.aria-describedby]="showError() ? 'nl-error' : null"
              />
              <button type="submit" class="btn" [disabled]="sending()">S'inscrire</button>
            </div>
            @if (showError()) {
              <p id="nl-error" class="err">
                {{ error() || 'Saisissez une adresse e-mail valide.' }}
              </p>
            }
          }
        </form>
      </div>
    </section>

    <footer class="footer">
      <div class="container grid">
        <div class="about">
          <app-logo [size]="34" [light]="true" />
          <p>
            Fleuriste en ligne à Rabat. Des bouquets composés à la main chaque matin et livrés le
            jour même.
          </p>
          <div class="social">
            <a
              href="https://www.instagram.com/"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Bloomé sur Instagram (nouvel onglet)"
              ><app-icon name="instagram"
            /></a>
            <a
              href="https://www.facebook.com/"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Bloomé sur Facebook (nouvel onglet)"
              ><app-icon name="facebook"
            /></a>
            <a
              href="https://wa.me/212537000000"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Écrire à Bloomé sur WhatsApp (nouvel onglet)"
              ><app-icon name="whatsapp"
            /></a>
          </div>
        </div>
        <nav aria-label="Boutique">
          <h3>Boutique</h3>
          <a routerLink="/boutique" [queryParams]="{ categorie: 'bouquets' }">Bouquets</a>
          <a routerLink="/boutique" [queryParams]="{ categorie: 'roses' }">Roses</a>
          <a routerLink="/boutique" [queryParams]="{ categorie: 'plantes' }">Plantes d'intérieur</a>
          <a routerLink="/boutique" [queryParams]="{ categorie: 'coffrets' }">Coffrets cadeaux</a>
          <a routerLink="/creer-mon-bouquet">Créer mon bouquet</a>
        </nav>
        <nav aria-label="Aide">
          <h3>Aide</h3>
          <a routerLink="/livraison">Livraison</a>
          <a routerLink="/faq">Questions fréquentes</a>
          <a routerLink="/contact">Contact</a>
          <a routerLink="/compte/commandes">Suivre ma commande</a>
        </nav>
        <div class="contact">
          <h3>Nous trouver</h3>
          <p><app-icon name="pin" [size]="18" /> 14 avenue Fal Ould Oumeir, Agdal, Rabat</p>
          <p>
            <app-icon name="phone" [size]="18" /> <a href="tel:+212537000000">05 37 00 00 00</a>
          </p>
          <p>
            <app-icon name="mail" [size]="18" />
            <a href="mailto:bonjour@bloome.ma">bonjour&#64;bloome.ma</a>
          </p>
          <p><app-icon name="clock" [size]="18" /> Lun – Sam, 9h – 19h</p>
        </div>
      </div>
      <div class="container legal">
        <p>© {{ year }} Bloomé. Tous droits réservés.</p>
        <nav aria-label="Informations légales">
          <a routerLink="/cgv">Conditions de vente</a>
          <a routerLink="/confidentialite">Confidentialité</a>
          <a routerLink="/a-propos">À propos</a>
        </nav>
      </div>
    </footer>
  `,
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  private readonly catalog = inject(CatalogService);

  protected readonly year = new Date().getFullYear();
  protected readonly email = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.email],
  });
  protected readonly sending = signal(false);
  protected readonly done = signal('');
  protected readonly error = signal('');
  protected readonly submitted = signal(false);

  protected showError(): boolean {
    return (this.submitted() && this.email.invalid) || this.error() !== '';
  }

  subscribe(event: Event): void {
    event.preventDefault();
    this.submitted.set(true);
    this.error.set('');
    if (this.email.invalid) return;
    this.sending.set(true);
    this.catalog.subscribe(this.email.value.trim()).subscribe({
      next: (res) => {
        this.done.set(res.message);
        this.sending.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.sending.set(false);
      },
    });
  }
}
