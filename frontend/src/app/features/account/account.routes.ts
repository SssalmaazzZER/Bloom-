import { Routes } from '@angular/router';
import { AccountLayoutComponent } from './account-layout.component';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    component: AccountLayoutComponent,
    children: [
      {
        path: '',
        title: 'Mon compte',
        loadComponent: () => import('./profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'commandes',
        title: 'Mes commandes',
        loadComponent: () => import('./orders.component').then((m) => m.OrdersComponent),
      },
      {
        path: 'commandes/:id',
        title: 'Ma commande',
        loadComponent: () => import('./order-detail.component').then((m) => m.OrderDetailComponent),
      },
      {
        path: 'adresses',
        title: 'Mes adresses',
        loadComponent: () => import('./addresses.component').then((m) => m.AddressesComponent),
      },
      {
        path: 'favoris',
        title: 'Mes favoris',
        loadComponent: () => import('./wishlist.component').then((m) => m.WishlistComponent),
      },
      {
        path: 'securite',
        title: 'Sécurité',
        loadComponent: () => import('./security.component').then((m) => m.SecurityComponent),
      },
    ],
  },
];
