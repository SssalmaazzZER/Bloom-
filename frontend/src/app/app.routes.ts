import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/guards/auth.guards';
import { ShopLayoutComponent } from './layout/shop-layout.component';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    component: ShopLayoutComponent,
    children: [
      {
        path: '',
        title: '',
        loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'boutique',
        title: 'Boutique',
        loadComponent: () =>
          import('./features/catalog/catalog.component').then((m) => m.CatalogComponent),
      },
      {
        path: 'produit/:slug',
        title: 'Produit',
        loadComponent: () =>
          import('./features/product/product.component').then((m) => m.ProductComponent),
      },
      {
        path: 'occasions',
        title: 'Occasions',
        loadComponent: () =>
          import('./features/occasions/occasions.component').then((m) => m.OccasionsComponent),
      },
      {
        path: 'creer-mon-bouquet',
        title: 'Créer mon bouquet',
        loadComponent: () =>
          import('./features/builder/builder.component').then((m) => m.BuilderComponent),
      },
      {
        path: 'panier',
        title: 'Panier',
        loadComponent: () => import('./features/cart/cart.component').then((m) => m.CartComponent),
      },
      {
        path: 'commande',
        title: 'Commande',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent),
      },
      {
        path: 'commande/confirmation/:id',
        title: 'Commande confirmée',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/checkout/confirmation.component').then((m) => m.ConfirmationComponent),
      },
      {
        path: 'connexion',
        title: 'Connexion',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'inscription',
        title: 'Créer un compte',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'compte',
        canActivate: [authGuard],
        loadChildren: () =>
          import('./features/account/account.routes').then((m) => m.ACCOUNT_ROUTES),
      },
      {
        path: 'a-propos',
        title: 'À propos',
        data: { page: 'about' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: 'livraison',
        title: 'Livraison',
        data: { page: 'delivery' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: 'contact',
        title: 'Contact',
        data: { page: 'contact' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: 'faq',
        title: 'Questions fréquentes',
        data: { page: 'faq' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: 'cgv',
        title: 'Conditions de vente',
        data: { page: 'terms' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: 'confidentialite',
        title: 'Confidentialité',
        data: { page: 'privacy' },
        loadComponent: () => import('./features/info/info.component').then((m) => m.InfoComponent),
      },
      {
        path: '**',
        title: 'Page introuvable',
        loadComponent: () =>
          import('./features/info/not-found.component').then((m) => m.NotFoundComponent),
      },
    ],
  },
];
