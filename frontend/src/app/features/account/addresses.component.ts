import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { UiService } from '../../core/services/ui.service';
import { errorMessage } from '../../core/errors';
import { Address } from '../../models/api.models';
import { phoneValidator } from '../../shared/validators/validators';
import { EmptyStateComponent, ErrorStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-addresses',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, EmptyStateComponent, ErrorStateComponent],
  template: `
    <div class="head">
      <h2>Mes adresses</h2>
      @if (!editing()) {
        <button type="button" class="btn btn-sm" (click)="startCreate()">
          Ajouter une adresse
        </button>
      }
    </div>

    @if (editing()) {
      <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h3>{{ editingId() ? "Modifier l'adresse" : 'Nouvelle adresse' }}</h3>
        @if (error()) {
          <p class="alert" role="alert">{{ error() }}</p>
        }
        <div class="form-grid">
          <div class="field">
            <label for="label">Nom de l'adresse</label>
            <input
              id="label"
              class="input"
              formControlName="label"
              placeholder="Maison, Bureau, Maman…"
            />
            @if (invalid('label')) {
              <span class="error">Donnez un nom à cette adresse.</span>
            }
          </div>
          <div class="field">
            <label for="recipientName">Destinataire</label>
            <input
              id="recipientName"
              class="input"
              formControlName="recipientName"
              autocomplete="name"
            />
            @if (invalid('recipientName')) {
              <span class="error">Le nom du destinataire est requis.</span>
            }
          </div>
          <div class="field">
            <label for="phone">Téléphone</label>
            <input id="phone" class="input" type="tel" formControlName="phone" autocomplete="tel" />
            @if (invalid('phone')) {
              <span class="error">Numéro invalide (ex. 06 12 34 56 78).</span>
            }
          </div>
          <div class="field">
            <label for="city">Ville</label>
            <input id="city" class="input" formControlName="city" autocomplete="address-level2" />
            @if (invalid('city')) {
              <span class="error">La ville est requise.</span>
            }
          </div>
          <div class="field span-2">
            <label for="line1">Adresse</label>
            <input id="line1" class="input" formControlName="line1" autocomplete="address-line1" />
            @if (invalid('line1')) {
              <span class="error">L'adresse est requise.</span>
            }
          </div>
          <div class="field">
            <label for="line2">Complément</label>
            <input id="line2" class="input" formControlName="line2" autocomplete="address-line2" />
          </div>
          <div class="field">
            <label for="postalCode">Code postal</label>
            <input
              id="postalCode"
              class="input"
              formControlName="postalCode"
              inputmode="numeric"
              maxlength="5"
              autocomplete="postal-code"
            />
            @if (invalid('postalCode')) {
              <span class="error">5 chiffres.</span>
            }
          </div>
        </div>
        <label class="check"
          ><input type="checkbox" formControlName="defaultAddress" />
          <span>Adresse par défaut</span></label
        >
        <div class="actions">
          <button type="button" class="btn btn-ghost" (click)="editing.set(false)">Annuler</button>
          <button type="submit" class="btn" [disabled]="saving()">Enregistrer l'adresse</button>
        </div>
      </form>
    }

    @if (failed()) {
      <app-error-state title="Vos adresses n'ont pas pu être chargées" (retry)="load()" />
    } @else if (addresses(); as list) {
      @if (list.length === 0 && !editing()) {
        <app-empty-state
          icon="pin"
          title="Aucune adresse enregistrée"
          message="Enregistrez vos adresses habituelles pour commander en quelques secondes."
        />
      } @else {
        <ul class="list">
          @for (a of list; track a.id) {
            <li class="card">
              <div>
                <strong>{{ a.label }}</strong>
                @if (a.defaultAddress) {
                  <span class="badge sage">Par défaut</span>
                }
                <p>
                  {{ a.recipientName }}<br />{{ a.line1 }}
                  @if (a.line2) {
                    , {{ a.line2 }}
                  }
                  <br />{{ a.postalCode }} {{ a.city }}<br />{{ a.phone }}
                </p>
              </div>
              <div class="row-actions">
                <button type="button" class="link-btn" (click)="startEdit(a)">Modifier</button>
                <button type="button" class="link-btn danger" (click)="remove(a)">Supprimer</button>
              </div>
            </li>
          }
        </ul>
      }
    }
  `,
  styles: `
    .head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-block-end: 18px;
      h2 {
        font-size: 1.6rem;
        margin: 0;
      }
    }
    form {
      margin-block-end: 20px;
      h3 {
        font-size: 1.3rem;
      }
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-block-start: 18px;
    }
    .list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr));
    }
    .list li {
      display: grid;
      gap: 10px;
      align-content: space-between;
      strong {
        font-family: var(--font-display);
        font-weight: 500;
        font-size: 1.15rem;
        color: var(--burgundy);
        margin-inline-end: 8px;
      }
      p {
        margin: 8px 0 0;
        font-size: 0.94rem;
        color: var(--ink-soft);
      }
    }
    .row-actions {
      display: flex;
      gap: 16px;
    }
    .danger {
      color: var(--danger);
    }
  `,
})
export class AddressesComponent {
  private readonly account = inject(AccountService);
  private readonly ui = inject(UiService);
  protected readonly addresses = signal<Address[] | null>(null);
  protected readonly failed = signal(false);
  protected readonly editing = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly form = new FormGroup({
    label: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(60)],
    }),
    recipientName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, phoneValidator],
    }),
    line1: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(200)],
    }),
    line2: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(200)] }),
    city: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    postalCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(\d{5})?$/)],
    }),
    defaultAddress: new FormControl(false, { nonNullable: true }),
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.failed.set(false);
    this.account
      .addresses()
      .subscribe({ next: (a) => this.addresses.set(a), error: () => this.failed.set(true) });
  }

  invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.touched && c.invalid;
  }

  startCreate(): void {
    this.form.reset();
    this.editingId.set(null);
    this.error.set('');
    this.editing.set(true);
  }

  startEdit(a: Address): void {
    this.form.reset({ ...a, line2: a.line2 ?? '', postalCode: a.postalCode ?? '' });
    this.editingId.set(a.id);
    this.error.set('');
    this.editing.set(true);
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const body = { ...v, line2: v.line2 || undefined, postalCode: v.postalCode || undefined };
    const id = this.editingId();
    const req$ = id ? this.account.updateAddress(id, body) : this.account.createAddress(body);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        this.ui.toast('Adresse enregistrée.');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  async remove(a: Address): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Supprimer cette adresse ?',
      message: `« ${a.label} » sera supprimée de votre carnet.`,
      confirmLabel: 'Supprimer',
      danger: true,
    });
    if (!ok) return;
    this.account.deleteAddress(a.id).subscribe({
      next: () => {
        this.ui.toast('Adresse supprimée.', 'info');
        this.load();
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }
}
