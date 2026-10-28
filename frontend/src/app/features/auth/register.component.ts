import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage, fieldErrors } from '../../core/errors';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';
import { phoneValidator, strongPassword } from '../../shared/validators/validators';
import { safeReturnUrl } from './login.component';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ImgFallbackDirective],
  template: `
    <div class="auth">
      <div class="visual" aria-hidden="true">
        <img
          appImgFallback
          src="https://images.unsplash.com/photo-1561181286-d3fee7d55364?auto=format&fit=crop&w=1200&q=80"
          alt=""
        />
      </div>
      <div class="panel">
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <h1>Créer un compte</h1>
          <p class="lede">
            Suivez vos livraisons, enregistrez vos adresses et retrouvez vos coups de cœur.
          </p>
          @if (error()) {
            <p class="alert" role="alert">{{ error() }}</p>
          }
          <div class="form-grid">
            <div class="field">
              <label for="firstName">Prénom</label>
              <input
                id="firstName"
                class="input"
                formControlName="firstName"
                autocomplete="given-name"
              />
              @if (show('firstName')) {
                <span class="error">Le prénom est requis.</span>
              }
            </div>
            <div class="field">
              <label for="lastName">Nom</label>
              <input
                id="lastName"
                class="input"
                formControlName="lastName"
                autocomplete="family-name"
              />
              @if (show('lastName')) {
                <span class="error">Le nom est requis.</span>
              }
            </div>
          </div>
          <div class="field">
            <label for="email">E-mail</label>
            <input
              id="email"
              class="input"
              type="email"
              formControlName="email"
              autocomplete="email"
            />
            @if (show('email') || server()['email']) {
              <span class="error">{{
                server()['email'] || 'Saisissez une adresse e-mail valide.'
              }}</span>
            }
          </div>
          <div class="field">
            <label for="phone">Téléphone (facultatif)</label>
            <input
              id="phone"
              class="input"
              type="tel"
              formControlName="phone"
              autocomplete="tel"
              placeholder="06 12 34 56 78"
            />
            @if (show('phone')) {
              <span class="error">Numéro invalide (ex. 06 12 34 56 78).</span>
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
                autocomplete="new-password"
                aria-describedby="pw-rules"
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
            <ul id="pw-rules" class="rules">
              <li [class.ok]="rules().length">8 caractères minimum</li>
              <li [class.ok]="rules().letter">une lettre</li>
              <li [class.ok]="rules().digit">un chiffre</li>
            </ul>
          </div>
          <button type="submit" class="btn btn-block" [disabled]="loading()">
            {{ loading() ? 'Création…' : 'Créer mon compte' }}
          </button>
          <p class="switch">
            Déjà cliente ou client ?
            <a routerLink="/connexion" [queryParams]="{ returnUrl: returnUrl() }">Se connecter</a>
          </p>
        </form>
      </div>
    </div>
  `,
  styleUrl: './auth.scss',
})
export class RegisterComponent {
  readonly returnUrl = input<string>();
  private readonly auth = inject(AuthService);
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly server = signal<Record<string, string>>({});
  protected readonly showPw = signal(false);

  protected readonly form = new FormGroup({
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    phone: new FormControl('', { nonNullable: true, validators: [phoneValidator] }),
    password: new FormControl('', { nonNullable: true, validators: [strongPassword] }),
  });

  private readonly password = toSignal(this.form.controls.password.valueChanges, {
    initialValue: '',
  });
  protected readonly rules = computed(() => {
    const v = this.password();
    return { length: v.length >= 8, letter: /\p{L}/u.test(v), digit: /\d/.test(v) };
  });

  show(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.touched && c.invalid;
  }

  submit(): void {
    this.form.markAllAsTouched();
    this.server.set({});
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const v = this.form.getRawValue();
    this.auth
      .register({
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        email: v.email.trim(),
        password: v.password,
        phone: v.phone || undefined,
      })
      .subscribe({
        next: (user) => {
          this.loading.set(false);
          this.ui.toast(`Bienvenue ${user.firstName} ! Votre compte est créé.`);
          void this.router.navigateByUrl(safeReturnUrl(this.returnUrl()) ?? '/compte');
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(errorMessage(err));
          this.server.set(fieldErrors(err));
        },
      });
  }
}
