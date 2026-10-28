import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../core/services/admin.service';
import { CatalogService } from '../core/services/catalog.service';
import { UiService } from '../core/services/ui.service';
import { errorMessage } from '../core/errors';
import { Category } from '../models/api.models';
import { ImgFallbackDirective } from '../shared/directives/img-fallback.directive';
import { ImgSizePipe } from '../shared/pipes/labels.pipes';

@Component({
  selector: 'app-categories',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ImgFallbackDirective, ImgSizePipe],
  template: `
    <div class="head">
      <div>
        <h1>Catégories</h1>
        <p>Organisation de la boutique et des filtres.</p>
      </div>
      <button type="button" class="btn btn-sm" (click)="edit(null)">Nouvelle catégorie</button>
    </div>

    @if (editing()) {
      <form class="card editor" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingId() ? 'Modifier la catégorie' : 'Nouvelle catégorie' }}</h2>
        @if (error()) {
          <p class="alert" role="alert">{{ error() }}</p>
        }
        <div class="form-grid">
          <div class="field">
            <label for="c-name">Nom</label
            ><input id="c-name" class="input" formControlName="name" />
          </div>
          <div class="field">
            <label for="c-slug">Slug</label
            ><input id="c-slug" class="input" formControlName="slug" placeholder="fleurs-sechees" />
            @if (form.controls.slug.touched && form.controls.slug.invalid) {
              <span class="error">Minuscules, chiffres et tirets.</span>
            }
          </div>
          <div class="field span-2">
            <label for="c-desc">Description</label
            ><input id="c-desc" class="input" formControlName="description" />
          </div>
          <div class="field">
            <label for="c-img">Image (URL https://)</label
            ><input id="c-img" class="input" formControlName="imageUrl" />
          </div>
          <div class="field">
            <label for="c-order">Ordre d'affichage</label
            ><input id="c-order" class="input" type="number" min="0" formControlName="sortOrder" />
          </div>
        </div>
        <div class="row">
          <button type="button" class="btn btn-ghost btn-sm" (click)="editing.set(false)">
            Annuler
          </button>
          <button type="submit" class="btn btn-sm" [disabled]="saving()">Enregistrer</button>
        </div>
      </form>
    }

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th><span class="visually-hidden">Photo</span></th>
            <th>Catégorie</th>
            <th>Slug</th>
            <th class="num">Produits actifs</th>
            <th class="num">Ordre</th>
            <th><span class="visually-hidden">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          @for (c of categories(); track c.id) {
            <tr>
              <td><img class="thumb" appImgFallback [src]="c.imageUrl | imgSize: 120" alt="" /></td>
              <td>
                <strong>{{ c.name }}</strong
                ><br /><span class="muted small">{{ c.description }}</span>
              </td>
              <td>{{ c.slug }}</td>
              <td class="num">{{ c.productCount }}</td>
              <td class="num">{{ c.sortOrder }}</td>
              <td class="actions">
                <button type="button" class="link-btn" (click)="edit(c)">Modifier</button>
                <button type="button" class="link-btn danger" (click)="remove(c)">Supprimer</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styleUrls: ['./admin-shared.scss'],
  styles: `
    .editor {
      margin-block-end: 20px;
      h2 {
        font-size: 1.25rem;
      }
    }
    .row {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
    .small {
      font-size: 0.82rem;
    }
    .actions {
      white-space: nowrap;
      display: flex;
      gap: 14px;
    }
    .danger {
      color: var(--danger);
    }
  `,
})
export class CategoriesComponent {
  private readonly admin = inject(AdminService);
  private readonly catalog = inject(CatalogService);
  private readonly ui = inject(UiService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly editing = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    slug: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[a-z0-9]+(-[a-z0-9]+)*$/)],
    }),
    description: new FormControl('', { nonNullable: true }),
    imageUrl: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(https:\/\/\S+)?$/)],
    }),
    sortOrder: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.catalog.invalidate();
    this.catalog.categories().subscribe({
      next: (c) => this.categories.set(c),
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }

  edit(c: Category | null): void {
    this.error.set('');
    this.editingId.set(c?.id ?? null);
    this.form.reset(
      c
        ? {
            name: c.name,
            slug: c.slug,
            description: c.description ?? '',
            imageUrl: c.imageUrl ?? '',
            sortOrder: c.sortOrder,
          }
        : undefined,
    );
    this.editing.set(true);
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const body = {
      ...v,
      description: v.description || undefined,
      imageUrl: v.imageUrl || undefined,
    };
    const id = this.editingId();
    (id ? this.admin.updateCategory(id, body) : this.admin.createCategory(body)).subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        this.ui.toast('Catégorie enregistrée.');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  async remove(c: Category): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Supprimer la catégorie ?',
      message: `« ${c.name} » sera supprimée. Impossible si elle contient des produits.`,
      confirmLabel: 'Supprimer',
      danger: true,
    });
    if (!ok) return;
    this.admin.deleteCategory(c.id).subscribe({
      next: () => {
        this.ui.toast('Catégorie supprimée.', 'info');
        this.load();
      },
      error: (err) => this.ui.error(errorMessage(err)),
    });
  }
}
