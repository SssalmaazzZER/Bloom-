import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * - Sends cookies with API calls (same-origin in dev through the proxy; also works cross-origin).
 * - Reacts to a session that expired server-side: clears local auth state and, for protected
 *   areas, sends the user to the login page with a return URL.
 * CSRF: Angular's built-in XSRF support copies the XSRF-TOKEN cookie into X-XSRF-TOKEN.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const request = req.url.startsWith('/api/') ? req.clone({ withCredentials: true }) : req;

  return next(request).pipe(
    catchError((err: unknown) => {
      if (
        err instanceof HttpErrorResponse &&
        err.status === 401 &&
        !req.url.endsWith('/auth/me') &&
        !req.url.endsWith('/auth/login')
      ) {
        if (auth.isLoggedIn()) {
          auth.clearSession();
          const url = router.url;
          if (
            url.startsWith('/compte') ||
            url.startsWith('/commande') ||
            url.startsWith('/admin')
          ) {
            void router.navigate(['/connexion'], { queryParams: { returnUrl: url, expired: 1 } });
          }
        }
      }
      return throwError(() => err);
    }),
  );
};
