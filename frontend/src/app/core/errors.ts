import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.models';

/** Extracts the API error body from any error thrown by HttpClient (or our local cart). */
export function apiError(err: unknown): Partial<ApiError> {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return {
        code: 'NETWORK',
        message: 'Connexion au serveur impossible. Vérifiez votre connexion et réessayez.',
      };
    }
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && 'message' in body) return body;
    return { code: 'HTTP_' + err.status, message: 'Une erreur est survenue. Merci de réessayer.' };
  }
  if (err && typeof err === 'object' && 'error' in err) {
    return (err as { error: Partial<ApiError> }).error;
  }
  return { code: 'UNKNOWN', message: 'Une erreur est survenue. Merci de réessayer.' };
}

export function errorMessage(err: unknown): string {
  return apiError(err).message ?? 'Une erreur est survenue. Merci de réessayer.';
}

/** Maps API field errors onto a { field: message } dictionary for forms. */
export function fieldErrors(err: unknown): Record<string, string> {
  const result: Record<string, string> = {};
  for (const fe of apiError(err).fieldErrors ?? []) {
    result[fe.field] ??= fe.message;
  }
  return result;
}
