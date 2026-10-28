import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CatalogService } from '../../core/services/catalog.service';
import { MadPipe } from '../../shared/pipes/mad.pipe';
import { IconComponent } from '../../shared/components/icon.component';
import { ImgFallbackDirective } from '../../shared/directives/img-fallback.directive';

type Page = 'about' | 'delivery' | 'contact' | 'faq' | 'terms' | 'privacy';

const TITLES: Record<Page, string> = {
  about: 'Notre histoire',
  delivery: 'Livraison',
  contact: 'Nous contacter',
  faq: 'Questions fréquentes',
  terms: 'Conditions générales de vente',
  privacy: 'Politique de confidentialité',
};

@Component({
  selector: 'app-info',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MadPipe, IconComponent, ImgFallbackDirective],
  templateUrl: './info.component.html',
  styleUrl: './info.component.scss',
})
export class InfoComponent {
  /** Route data `page`, bound by withComponentInputBinding. */
  readonly page = input.required<Page>();
  private readonly catalog = inject(CatalogService);

  protected readonly title = computed(() => TITLES[this.page()]);
  protected readonly delivery = toSignal(
    this.catalog.deliveryOptions().pipe(catchError(() => of(null))),
  );

  protected readonly faq = [
    {
      q: 'Jusqu’à quelle heure puis-je commander pour une livraison le jour même ?',
      a: 'Toute commande validée avant 14h (heure du Maroc) peut être livrée le jour même, sur les créneaux de l’après-midi ou de la soirée.',
    },
    {
      q: 'Comment payer ?',
      a: 'Le paiement se fait à la livraison, en espèces, auprès de notre coursier. Aucune information bancaire n’est demandée en ligne.',
    },
    {
      q: 'Puis-je annuler ma commande ?',
      a: 'Oui, depuis votre espace client, tant que la commande est « Reçue » ou « Confirmée ». Une fois en préparation, contactez-nous par téléphone.',
    },
    {
      q: 'Les photos correspondent-elles au bouquet livré ?',
      a: 'Nos bouquets suivent fidèlement la composition indiquée. Selon les arrivages, une fleur peut être remplacée par une variété de même couleur et de valeur égale ou supérieure.',
    },
    {
      q: 'Combien de temps tiennent les fleurs ?',
      a: 'Nous garantissons sept jours de fraîcheur si les conseils d’entretien sont suivis. Sinon, nous remplaçons le bouquet.',
    },
    {
      q: 'Puis-je ajouter un message ?',
      a: 'Oui : une carte manuscrite est offerte avec chaque commande. Saisissez votre message à l’étape de commande ou dans l’atelier « Créer mon bouquet ».',
    },
  ];
}
