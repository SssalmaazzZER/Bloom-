/** TypeScript mirrors of the backend DTOs (see backend/src/main/java/ma/bloome/dto). */

export type FlowerColor =
  'RED' | 'PINK' | 'WHITE' | 'YELLOW' | 'ORANGE' | 'PURPLE' | 'BLUE' | 'GREEN' | 'PASTEL' | 'MIXED';

export type ProductSort =
  'POPULARITY' | 'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC' | 'NAME_ASC' | 'NAME_DESC';

export type OrderStatus =
  'PENDING' | 'CONFIRMED' | 'PREPARING' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';

export type DeliverySlot = 'MORNING' | 'AFTERNOON' | 'EVENING';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}

export interface Category {
  id: number;
  slug: string;
  name: string;
  description?: string;
  imageUrl?: string;
  sortOrder: number;
  productCount: number;
}

export interface Occasion {
  id: number;
  slug: string;
  name: string;
  description?: string;
  imageUrl?: string;
}

export interface ImageRef {
  url: string;
  altText?: string;
}

export interface ProductSummary {
  id: number;
  slug: string;
  name: string;
  shortDescription: string;
  price: number;
  compareAtPrice?: number;
  imageUrl?: string;
  categorySlug: string;
  categoryName: string;
  inStock: boolean;
  lowStock: boolean;
  availableQuantity: number;
  featured: boolean;
  bestseller: boolean;
  colors: FlowerColor[];
  ratingAverage?: number;
  ratingCount: number;
  createdAt: string;
}

export interface DeliveryEstimate {
  earliestDate: string;
  sameDay: boolean;
  message: string;
  deliveryFee: number;
  freeDeliveryThreshold: number;
}

export interface ProductDetail {
  id: number;
  slug: string;
  name: string;
  shortDescription: string;
  description: string;
  composition?: string;
  careInstructions?: string;
  price: number;
  compareAtPrice?: number;
  category: Category;
  images: ImageRef[];
  colors: FlowerColor[];
  occasions: Occasion[];
  inStock: boolean;
  lowStock: boolean;
  availableQuantity: number;
  featured: boolean;
  bestseller: boolean;
  ratingAverage?: number;
  ratingCount: number;
  deliveryEstimate: DeliveryEstimate;
}

export interface CatalogFacets {
  minPrice: number;
  maxPrice: number;
  colors: FlowerColor[];
  categories: Category[];
  occasions: Occasion[];
}

export interface ProductQuery {
  q?: string;
  category?: string[];
  occasion?: string[];
  color?: FlowerColor[];
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sort?: ProductSort;
  page?: number;
  size?: number;
}

/* ---------- bouquet builder ---------- */
export interface StemOption {
  id: number;
  name: string;
  color: FlowerColor;
  description?: string;
  imageUrl?: string;
  unitPrice: number;
  maxPerBouquet: number;
  available: number;
}

export interface WrappingOption {
  id: number;
  name: string;
  description?: string;
  swatchColor: string;
  price: number;
}

export interface RibbonOption {
  id: number;
  name: string;
  swatchColor: string;
  price: number;
}

export interface BouquetOptions {
  stems: StemOption[];
  wrappings: WrappingOption[];
  ribbons: RibbonOption[];
  minStems: number;
  maxStems: number;
  currency: string;
}

export interface BouquetRequest {
  wrappingId: number;
  ribbonId: number;
  message?: string;
  stems: { stemId: number; quantity: number }[];
}

/* ---------- cart ---------- */
export interface CartItemRequest {
  productId?: number;
  bouquet?: BouquetRequest;
  quantity: number;
  clientKey?: string;
}

export type LineIssue = 'UNAVAILABLE' | 'OUT_OF_STOCK' | 'QUANTITY_EXCEEDS_STOCK';

export interface BouquetStemLine {
  stemId?: number;
  name: string;
  color: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface BouquetSummary {
  wrappingId?: number;
  wrappingName: string;
  wrappingColor?: string;
  wrappingPrice: number;
  ribbonId?: number;
  ribbonName: string;
  ribbonColor?: string;
  ribbonPrice: number;
  message?: string;
  stems: BouquetStemLine[];
  stemCount: number;
  unitPrice: number;
}

export interface CartLine {
  id: string;
  type: 'PRODUCT' | 'CUSTOM_BOUQUET' | 'UNKNOWN';
  productId?: number;
  slug?: string;
  name: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  maxQuantity: number;
  issue?: LineIssue;
  bouquet?: BouquetSummary;
}

export interface CartView {
  items: CartLine[];
  itemCount: number;
  subtotal: number;
  deliveryFee: number;
  giftWrappingFee: number;
  discount: number;
  total: number;
  freeDeliveryThreshold: number;
  amountToFreeDelivery: number;
  checkoutReady: boolean;
  currency: string;
}

/* ---------- delivery & orders ---------- */
export interface DeliveryOptions {
  cities: string[];
  slots: { code: DeliverySlot; label: string; startHour: number; endHour: number }[];
  earliestDate: string;
  earliestDateSlots: DeliverySlot[];
  latestDate: string;
  deliveryFee: number;
  freeDeliveryThreshold: number;
  giftWrappingFee: number;
  sameDayCutoffHour: number;
  currency: string;
}

export interface CheckoutRequest {
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  recipientName?: string;
  recipientPhone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode?: string;
  deliveryInstructions?: string;
  giftMessage?: string;
  giftWrapping: boolean;
  deliveryDate: string;
  deliverySlot: DeliverySlot;
  paymentMethod: 'CASH_ON_DELIVERY';
  idempotencyKey: string;
  expectedTotal?: number;
}

export interface OrderItem {
  id: number;
  type: 'PRODUCT' | 'CUSTOM_BOUQUET';
  productId?: number;
  productSlug?: string;
  name: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  bouquet?: BouquetSummary;
}

export interface OrderSummary {
  id: number;
  reference: string;
  status: OrderStatus;
  createdAt: string;
  total: number;
  itemCount: number;
  deliveryDate: string;
  deliverySlot: DeliverySlot;
  previewImageUrl?: string;
  customerName: string;
  city: string;
}

export interface OrderDetail {
  id: number;
  reference: string;
  status: OrderStatus;
  paymentMethod: string;
  paymentStatus: string;
  createdAt: string;
  cancelledAt?: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  recipientName: string;
  recipientPhone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode?: string;
  deliveryInstructions?: string;
  giftMessage?: string;
  giftWrapping: boolean;
  deliveryDate: string;
  deliverySlot: DeliverySlot;
  deliverySlotLabel: string;
  items: OrderItem[];
  subtotal: number;
  deliveryFee: number;
  giftWrappingFee: number;
  discount: number;
  total: number;
  history: { status: OrderStatus; at: string; note?: string }[];
  cancellable: boolean;
  allowedTransitions: OrderStatus[];
}

/* ---------- account ---------- */
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt: string;
}

export interface Address {
  id: number;
  label: string;
  recipientName: string;
  phone: string;
  line1: string;
  line2?: string;
  city: string;
  postalCode?: string;
  defaultAddress: boolean;
}

export type AddressRequest = Omit<Address, 'id'>;

export interface Review {
  id: number;
  authorName: string;
  rating: number;
  title?: string;
  comment: string;
  createdAt: string;
  verifiedPurchase: boolean;
}

export interface ReviewPage {
  reviews: Page<Review>;
  average?: number;
  count: number;
  canReview: boolean;
  alreadyReviewed: boolean;
}

/* ---------- admin ---------- */
export interface AdminProduct {
  id: number;
  slug: string;
  name: string;
  shortDescription: string;
  description: string;
  composition?: string;
  careInstructions?: string;
  categoryId: number;
  categoryName: string;
  price: number;
  compareAtPrice?: number;
  stockQuantity: number;
  lowStockThreshold: number;
  maxPerOrder: number;
  active: boolean;
  featured: boolean;
  bestseller: boolean;
  salesCount: number;
  colors: FlowerColor[];
  occasionIds: number[];
  images: ImageRef[];
  lowStock: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminProductRequest {
  name: string;
  slug?: string;
  shortDescription: string;
  description: string;
  composition?: string;
  careInstructions?: string;
  categoryId: number;
  price: number;
  compareAtPrice?: number | null;
  stockQuantity: number;
  lowStockThreshold: number;
  maxPerOrder: number;
  active: boolean;
  featured: boolean;
  bestseller: boolean;
  colors: FlowerColor[];
  occasionIds: number[];
  images: ImageRef[];
  version?: number;
}

export interface Dashboard {
  totalOrders: number;
  revenue: number;
  deliveredRevenue: number;
  revenueDefinition: string;
  pendingOrders: number;
  totalCustomers: number;
  activeProducts: number;
  lowStockCount: number;
  ordersByStatus: Record<OrderStatus, number>;
  lowStock: {
    kind: 'PRODUCT' | 'STEM';
    id: number;
    name: string;
    stockQuantity: number;
    threshold: number;
  }[];
  recentOrders: OrderSummary[];
  bestsellers: { productId: number; name: string; quantitySold: number; revenue: number }[];
}

export interface CustomerSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  createdAt: string;
  enabled: boolean;
  orderCount: number;
  totalSpent: number;
}

export interface CustomerDetail {
  customer: CustomerSummary;
  orders: OrderSummary[];
}

export interface StemInventory {
  id: number;
  name: string;
  color: string;
  unitPrice: number;
  stockQuantity: number;
  active: boolean;
  lowStock: boolean;
}

export interface Inventory {
  products: AdminProduct[];
  stems: StemInventory[];
}
