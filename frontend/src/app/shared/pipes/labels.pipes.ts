import { Pipe, PipeTransform } from '@angular/core';
import { FlowerColor, OrderStatus } from '../../models/api.models';
import { COLOR_LABELS, PAYMENT_STATUS_LABELS, STATUS_LABELS } from '../../models/labels';

@Pipe({ name: 'statusLabel' })
export class StatusLabelPipe implements PipeTransform {
  transform(value: OrderStatus | string | null | undefined): string {
    return value ? (STATUS_LABELS[value as OrderStatus] ?? value) : '';
  }
}

@Pipe({ name: 'colorLabel' })
export class ColorLabelPipe implements PipeTransform {
  transform(value: FlowerColor | string | null | undefined): string {
    return value ? (COLOR_LABELS[value as FlowerColor] ?? value) : '';
  }
}

@Pipe({ name: 'paymentLabel' })
export class PaymentLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? (PAYMENT_STATUS_LABELS[value] ?? value) : '';
  }
}

/** Rewrites the width parameter of Unsplash URLs so thumbnails download smaller files. */
@Pipe({ name: 'imgSize' })
export class ImgSizePipe implements PipeTransform {
  transform(url: string | null | undefined, width: number): string | null {
    if (!url) return null;
    if (!url.startsWith('https://images.unsplash.com/')) return url;
    return url.includes('w=')
      ? url.replace(/([?&])w=\d+/, `$1w=${width}`)
      : `${url}${url.includes('?') ? '&' : '?'}w=${width}`;
  }
}
