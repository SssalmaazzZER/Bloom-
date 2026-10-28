import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { phoneValidator } from '../../shared/validators/validators';

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
      <h2>Mes informations</h2>
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
          @if (form.controls.firstName.touched && form.controls.firstName.invalid) {
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
          @if (form.controls.lastName.touched && form.controls.lastName.invalid) {
            <span class="error">Le nom est requis.</span>
          }
        </div>
        <div class="field">
          <label for="email">E-mail</label>
          <input id="email" class="input" [value]="auth.user()?.email" disabled />
          <span class="hint"
            >L'adresse e-mail sert d'identifiant et ne peut pas être modifiée ici.</span
          >
        </div>
        <div class="field">
          <label for="phone">Téléphone</label>
          <input id="phone" class="input" type="tel" formControlName="phone" autocomplete="tel" />
          @if (form.controls.phone.touched && form.controls.phone.invalid) {
            <span class="error">Numéro invalide (ex. 06 12 34 56 78).</span>
          }
        </div>
      </div>
      <button type="submit" class="btn" [disabled]="saving() || form.pristine">
        Enregistrer les modifications
      </button>
      <p class="muted small">Client depuis le {{ auth.user()?.createdAt | date: 'd MMMM y' }}.</p>
    </form>

    <div class="shortcuts">
      <a class="card" routerLink="/compte/commandes"
        ><strong>Mes commandes</strong
        ><span class="muted">Suivre une livraison, annuler une commande</span></a
      >
      <a class="card" routerLink="/compte/adresses"
        ><strong>Mes adresses</strong><span class="muted">Pour commander plus vite</span></a
      >
      <a class="card" routerLink="/compte/favoris"
        ><strong>Mes favoris</strong><span class="muted">Les créations que vous aimez</span></a
      >
    </div>
  `,
  styles: `
    h2 {
      font-size: 1.5rem;
    }
    .small {
      font-size: 0.86rem;
      margin: 14px 0 0;
    }
    .shortcuts {
      display: grid;
      gap: 14px;
      margin-block-start: 20px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 220px), 1fr));
      a {
        display: grid;
        gap: 4px;
        text-decoration: none;
        color: var(--ink);
        &:hover {
          border-color: var(--rose);
        }
        strong {
          font-family: var(--font-display);
          font-weight: 500;
          font-size: 1.15rem;
          color: var(--burgundy);
        }
        span {
          font-size: 0.9rem;
        }
      }
    }
  `,
})
export class ProfileComponent {
  protected readonly auth = inject(AuthService);
  private readonly account = inject(AccountService);
  private readonly ui = inject(UiService);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly form = new FormGroup({
    firstName: new FormControl(this.auth.user()?.firstName ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    lastName: new FormControl(this.auth.user()?.lastName ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    phone: new FormControl(this.auth.user()?.phone ?? '', {
      nonNullable: true,
      validators: [phoneValidator],
    }),
  });

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set('');
    const v = this.form.getRawValue();
    this.account
      .updateProfile({
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        phone: v.phone || undefined,
      })
      .subscribe({
        next: (user) => {
          this.auth.updateUser(user);
          this.saving.set(false);
          this.form.markAsPristine();
          this.ui.toast('Vos informations ont été enregistrées.');
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(errorMessage(err));
        },
      });
  }
}
