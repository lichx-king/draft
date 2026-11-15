import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DeliveryInfo, Invoice, OrderResult, ShippingFee } from '../models/cart-order.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);

  /** Bấm "Đặt hàng" ở giỏ: lỗi 409 INSUFFICIENT_STOCK nếu thiếu hàng. */
  checkStock(): Observable<{ ok: boolean }> {
    return this.http.post<{ ok: boolean }>('/api/orders/check-stock', {});
  }

  getShippingFee(province: string): Observable<ShippingFee> {
    return this.http.post<ShippingFee>('/api/orders/shipping-fee', { province });
  }

  /** Gửi thông tin giao hàng, nhận hóa đơn tạm. Lỗi 400 VALIDATION_ERROR có details = {field: message}. */
  submitDeliveryInfo(info: DeliveryInfo): Observable<Invoice> {
    return this.http.post<Invoice>('/api/orders/delivery-info', info);
  }

  getPendingInvoice(pendingOrderId: string): Observable<Invoice> {
    return this.http.get<Invoice>(`/api/orders/pending/${pendingOrderId}`);
  }

  /** CHỈ ĐỂ DEMO: giả lập thanh toán thành công. */
  devConfirm(pendingOrderId: string): Observable<OrderResult> {
    return this.http.post<OrderResult>(`/api/orders/pending/${pendingOrderId}/dev-confirm`, {});
  }

  getOrder(orderId: number): Observable<OrderResult> {
    return this.http.get<OrderResult>(`/api/orders/${orderId}`);
  }
}
