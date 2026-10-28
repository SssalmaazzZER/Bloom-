import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmptyStateComponent } from '../../shared/components/states.component';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, EmptyStateComponent],
  template: `
    <div class="container section">
      <h1 class="visually-hidden">Page introuvable</h1>
      <app-empty-state
        icon="leaf"
        title="Cette page s'est envolée"
        message="Le lien est peut-être incorrect ou la page a été déplacée."
      >
        <a class="btn" routerLink="/">Retour à l'accueil</a>
        <a class="btn btn-ghost" routerLink="/boutique">Voir la boutique</a>
      </app-empty-state>
    </div>
  `,
})
export class NotFoundComponent {}
