import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        title: 'Administration',
        loadComponent: () => import('./dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'commandes',
        title: 'Commandes (admin)',
        loadComponent: () => import('./orders.component').then((m) => m.AdminOrdersComponent),
      },
      {
        path: 'commandes/:id',
        title: 'Commande (admin)',
        loadComponent: () => import('./order.component').then((m) => m.AdminOrderComponent),
      },
      {
        path: 'produits',
        title: 'Produits (admin)',
        loadComponent: () => import('./products.component').then((m) => m.AdminProductsComponent),
      },
      {
        path: 'produits/nouveau',
        title: 'Nouveau produit',
        loadComponent: () => import('./product-form.component').then((m) => m.ProductFormComponent),
      },
      {
        path: 'produits/:id',
        title: 'Modifier le produit',
        loadComponent: () => import('./product-form.component').then((m) => m.ProductFormComponent),
      },
      {
        path: 'inventaire',
        title: 'Inventaire',
        loadComponent: () => import('./inventory.component').then((m) => m.InventoryComponent),
      },
      {
        path: 'categories',
        title: 'Catégories',
        loadComponent: () => import('./categories.component').then((m) => m.CategoriesComponent),
      },
      {
        path: 'clients',
        title: 'Clients',
        loadComponent: () => import('./customers.component').then((m) => m.CustomersComponent),
      },
      {
        path: 'clients/:id',
        title: 'Client',
        loadComponent: () => import('./customer.component').then((m) => m.CustomerComponent),
      },
    ],
  },
];
