import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminProduct,
  AdminProductRequest,
  Category,
  CustomerDetail,
  CustomerSummary,
  Dashboard,
  Inventory,
  OrderDetail,
  OrderStatus,
  OrderSummary,
  Page,
  StemInventory,
} from '../../models/api.models';
import { API } from './catalog.service';

const ADMIN = `${API}/admin`;

function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '') p = p.set(key, String(value));
  }
  return p;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${ADMIN}/dashboard`);
  }

  products(query: {
    q?: string;
    category?: string;
    active?: boolean | null;
    lowStock?: boolean;
    page?: number;
    size?: number;
  }): Observable<Page<AdminProduct>> {
    return this.http.get<Page<AdminProduct>>(`${ADMIN}/products`, { params: params(query) });
  }

  product(id: number): Observable<AdminProduct> {
    return this.http.get<AdminProduct>(`${ADMIN}/products/${id}`);
  }

  createProduct(body: AdminProductRequest): Observable<AdminProduct> {
    return this.http.post<AdminProduct>(`${ADMIN}/products`, body);
  }

  updateProduct(id: number, body: AdminProductRequest): Observable<AdminProduct> {
    return this.http.put<AdminProduct>(`${ADMIN}/products/${id}`, body);
  }

  deactivateProduct(id: number): Observable<AdminProduct> {
    return this.http.delete<AdminProduct>(`${ADMIN}/products/${id}`);
  }

  adjustStock(id: number, delta: number): Observable<AdminProduct> {
    return this.http.patch<AdminProduct>(`${ADMIN}/products/${id}/stock`, { delta });
  }

  adjustStemStock(id: number, delta: number): Observable<StemInventory> {
    return this.http.patch<StemInventory>(`${ADMIN}/stems/${id}/stock`, { delta });
  }

  inventory(): Observable<Inventory> {
    return this.http.get<Inventory>(`${ADMIN}/inventory`);
  }

  createCategory(body: Partial<Category>): Observable<Category> {
    return this.http.post<Category>(`${ADMIN}/categories`, body);
  }

  updateCategory(id: number, body: Partial<Category>): Observable<Category> {
    return this.http.put<Category>(`${ADMIN}/categories/${id}`, body);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN}/categories/${id}`);
  }

  orders(query: {
    status?: OrderStatus | '';
    q?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(`${ADMIN}/orders`, { params: params(query) });
  }

  order(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${ADMIN}/orders/${id}`);
  }

  updateStatus(id: number, status: OrderStatus, note?: string): Observable<OrderDetail> {
    return this.http.patch<OrderDetail>(`${ADMIN}/orders/${id}/status`, { status, note });
  }

  customers(query: {
    q?: string;
    page?: number;
    size?: number;
  }): Observable<Page<CustomerSummary>> {
    return this.http.get<Page<CustomerSummary>>(`${ADMIN}/customers`, { params: params(query) });
  }

  customer(id: number): Observable<CustomerDetail> {
    return this.http.get<CustomerDetail>(`${ADMIN}/customers/${id}`);
  }
}
