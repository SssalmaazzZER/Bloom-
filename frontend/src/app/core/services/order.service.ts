import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckoutRequest, OrderDetail, OrderSummary, Page } from '../../models/api.models';
import { API } from './catalog.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  checkout(request: CheckoutRequest): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${API}/orders`, request);
  }

  myOrders(page = 0, size = 10): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(`${API}/orders/my`, { params: { page, size } });
  }

  get(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${API}/orders/${id}`);
  }

  cancel(id: number, reason?: string): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${API}/orders/${id}/cancel`, { reason });
  }
}
