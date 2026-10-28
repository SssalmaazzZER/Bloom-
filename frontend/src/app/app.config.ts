import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
  withRouterConfig,
} from '@angular/router';
import { routes } from './app.routes';
import { apiInterceptor } from './core/interceptors/api.interceptor';
import { AuthService } from './core/services/auth.service';
import { BloomeTitleStrategy } from './core/title.strategy';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
      withRouterConfig({ paramsInheritanceStrategy: 'always' }),
    ),
    provideHttpClient(
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
      withInterceptors([apiInterceptor]),
    ),
    { provide: LOCALE_ID, useValue: 'fr' },
    { provide: TitleStrategy, useClass: BloomeTitleStrategy },
    provideAppInitializer(() => inject(AuthService).init()),
  ],
};
