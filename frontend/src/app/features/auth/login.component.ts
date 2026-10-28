import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AccountService } from '../../core/services/account.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';

/** Only same-site relative paths are accepted as post-login destinations (no open redirects). */
export function safeReturnUrl(url: string | undefined | null): string | null {
  return url && url.startsWith('/') && !url.startsWith('//') && !url.includes('://') ? url : null;
}

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ImgFallbackDirective],
  template: `
    <div class="auth">
      <div class="visual" aria-hidden="true">
        <img
          appImgFallback
          src="https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?auto=format&fit=crop&w=1200&q=80"
          alt=""
        />
      </div>
      <div class="panel">
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <h1>Bon retour parmi nous</h1>
          <p class="lede">Connectez-vous pour suivre vos commandes et retrouver vos favoris.</p>
          @if (expired()) {
            <p class="alert info" role="status">
              Votre session a expiré. Connectez-vous à nouveau pour continuer.
            </p>
          }
          @if (panier()) {
            <p class="alert info" role="status">
              Connectez-vous ou créez un compte pour finaliser votre commande. Votre panier est
              conservé.
            </p>
          }
          @if (error()) {
            <p class="alert" role="alert">{{ error() }}</p>
          }
          <div class="field">
            <label for="email">E-mail</label>
            <input
              id="email"
              class="input"
              type="email"
              formControlName="email"
              autocomplete="email"
            />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="error">Saisissez une adresse e-mail valide.</span>
            }
          </div>
          <div class="field">
            <label for="password">Mot de passe</label>
            <div class="pw">
              <input
                id="password"
                class="input"
                [type]="showPw() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="current-password"
              />
              <button
                type="button"
                class="toggle"
                (click)="showPw.set(!showPw())"
                [attr.aria-pressed]="showPw()"
              >
                {{ showPw() ? 'Masquer' : 'Afficher' }}
              </button>
            </div>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <span class="error">Saisissez votre mot de passe.</span>
            }
          </div>
          <button type="submit" class="btn btn-block" [disabled]="loading()">
            {{ loading() ? 'Connexion…' : 'Se connecter' }}
          </button>
          <p class="switch">
            Pas encore de compte ?
            <a routerLink="/inscription" [queryParams]="{ returnUrl: returnUrl() }"
              >Créer un compte</a
            >
          </p>
        </form>
      </div>
    </div>
  `,
  styleUrl: './auth.scss',
})
export class LoginComponent {
  readonly returnUrl = input<string>();
  readonly expired = input<string>();
  readonly panier = input<string>();

  private readonly auth = inject(AuthService);
  private readonly account = inject(AccountService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly showPw = signal(false);

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: (user) => {
        this.loading.set(false);
        this.ui.toast(`Bonjour ${user.firstName} !`);
        this.account.wishlist().subscribe({ error: () => undefined });
        const target =
          safeReturnUrl(this.returnUrl()) ?? (user.role === 'ADMIN' ? '/admin' : '/compte');
        void this.router.navigateByUrl(target);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
        this.form.controls.password.reset();
      },
    });
  }
}
