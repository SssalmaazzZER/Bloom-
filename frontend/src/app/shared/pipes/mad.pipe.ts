import { Pipe, PipeTransform } from '@angular/core';

const withDecimals = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const whole = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });

/** Formats a price in Moroccan dirhams: "590 MAD", "315,50 MAD", "1 425 MAD". */
export function formatMad(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—';
  const n = Number(value);
  const text = Number.isInteger(Math.round(n * 100) / 100)
    ? whole.format(n)
    : withDecimals.format(n);
  return `${text}\u00a0MAD`; // non-breaking space keeps the amount and currency together
}

@Pipe({ name: 'mad' })
export class MadPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatMad(value);
  }
}
