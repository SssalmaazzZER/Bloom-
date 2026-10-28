import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { strongPassword } from '../../shared/validators/validators';

function matching(group: AbstractControl): ValidationErrors | null {
  return group.get('newPassword')?.value === group.get('confirm')?.value
    ? null
    : { mismatch: true };
}

@Component({
  selector: 'app-security',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
      <h2>Changer mon mot de passe</h2>
      @if (error()) {
        <p class="alert" role="alert">{{ error() }}</p>
      }
      <div class="field">
        <label for="current">Mot de passe actuel</label>
        <input
          id="current"
          class="input"
          type="password"
          formControlName="currentPassword"
          autocomplete="current-password"
        />
      </div>
      <div class="field">
        <label for="new">Nouveau mot de passe</label>
        <input
          id="new"
          class="input"
          type="password"
          formControlName="newPassword"
          autocomplete="new-password"
        />
        <span class="hint">8 caractères minimum, dont une lettre et un chiffre.</span>
        @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
          <span class="error">Le mot de passe ne respecte pas les règles.</span>
        }
      </div>
      <div class="field">
        <label for="confirm">Confirmer le nouveau mot de passe</label>
        <input
          id="confirm"
          class="input"
          type="password"
          formControlName="confirm"
          autocomplete="new-password"
        />
        @if (form.controls.confirm.touched && form.hasError('mismatch')) {
          <span class="error">Les deux mots de passe ne correspondent pas.</span>
        }
      </div>
      <button type="submit" class="btn" [disabled]="saving()">Mettre à jour</button>
    </form>
  `,
  styles: `
    h2 {
      font-size: 1.5rem;
    }
    form {
      max-width: 520px;
    }
  `,
})
export class SecurityComponent {
  private readonly account = inject(AccountService);
  private readonly ui = inject(UiService);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly form = new FormGroup(
    {
      currentPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      newPassword: new FormControl('', { nonNullable: true, validators: [strongPassword] }),
      confirm: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: matching },
  );

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set('');
    const v = this.form.getRawValue();
    this.account.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => {
        this.saving.set(false);
        this.form.reset();
        this.ui.toast('Votre mot de passe a été modifié.');
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
