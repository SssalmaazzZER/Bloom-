import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** UX guards only: the API enforces every permission independently. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.whenReady();
  return auth.isLoggedIn()
    ? true
    : router.createUrlTree(['/connexion'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.whenReady();
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/connexion'], { queryParams: { returnUrl: state.url } });
  }
  return auth.isAdmin() ? true : router.createUrlTree(['/']);
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.whenReady();
  return auth.isLoggedIn() ? router.createUrlTree(['/compte']) : true;
};
