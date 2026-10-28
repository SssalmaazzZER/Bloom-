import { FlowerColor, OrderStatus } from './api.models';

/** French UI labels for backend enum values (the API stays in English). */
export const COLOR_LABELS: Record<FlowerColor, string> = {
  RED: 'Rouge',
  PINK: 'Rose',
  WHITE: 'Blanc',
  YELLOW: 'Jaune',
  ORANGE: 'Orange',
  PURPLE: 'Violet',
  BLUE: 'Bleu',
  GREEN: 'Vert',
  PASTEL: 'Pastel',
  MIXED: 'Multicolore',
};

export const COLOR_SWATCHES: Record<FlowerColor, string> = {
  RED: '#a3222f',
  PINK: '#e59aa7',
  WHITE: '#ffffff',
  YELLOW: '#f2c53d',
  ORANGE: '#ec8a3c',
  PURPLE: '#8a5aa8',
  BLUE: '#4f6fb3',
  GREEN: '#6f8a5c',
  PASTEL: 'linear-gradient(135deg, #f6d6dc, #e9e4f5 50%, #fdf1d6)',
  MIXED: 'conic-gradient(#a3222f, #f2c53d, #6f8a5c, #4f6fb3, #e59aa7, #a3222f)',
};

export const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Reçue',
  CONFIRMED: 'Confirmée',
  PREPARING: 'En préparation',
  OUT_FOR_DELIVERY: 'En livraison',
  DELIVERED: 'Livrée',
  CANCELLED: 'Annulée',
};

/** Ordered steps shown in the customer tracking timeline. */
export const STATUS_FLOW: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PREPARING',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

export const PAYMENT_STATUS_LABELS: Record<string, string> = {
  UNPAID: 'À régler à la livraison',
  PAID: 'Payée',
  REFUNDED: 'Remboursée',
  VOID: 'Annulé',
};

export const SORT_LABELS: Record<string, string> = {
  POPULARITY: 'Les plus populaires',
  NEWEST: 'Nouveautés',
  PRICE_ASC: 'Prix croissant',
  PRICE_DESC: 'Prix décroissant',
  NAME_ASC: 'Nom (A → Z)',
  NAME_DESC: 'Nom (Z → A)',
};
