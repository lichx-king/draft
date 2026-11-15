import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Cart, DevProduct } from '../models/cart-order.models';

/** Gọi API giỏ hàng và giữ sẵn "số lượng trong giỏ" để Header hiển thị. */
@Injectable({ providedIn: 'root' })
export class CartService {
  private http = inject(HttpClient);

  private countSubject = new BehaviorSubject<number>(0);
  /** Header dùng: {{ cartService.count$ | async }} */
  readonly count$ = this.countSubject.asObservable();

  load(): Observable<Cart> {
    return this.http.get<Cart>('/api/cart').pipe(tap((c) => this.updateCount(c)));
  }

  add(productId: number, quantity: number): Observable<Cart> {
    return this.http
      .post<Cart>('/api/cart/items', { productId, quantity })
      .pipe(tap((c) => this.updateCount(c)));
  }

  updateQuantity(productId: number, quantity: number): Observable<Cart> {
    return this.http
      .put<Cart>(`/api/cart/items/${productId}`, { quantity })
      .pipe(tap((c) => this.updateCount(c)));
  }

  remove(productId: number): Observable<Cart> {
    return this.http.delete<Cart>(`/api/cart/items/${productId}`).pipe(tap((c) => this.updateCount(c)));
  }

  /** Chỉ để test khi chưa có màn hình danh sách sản phẩm. */
  devProducts(): Observable<DevProduct[]> {
    return this.http.get<DevProduct[]>('/api/cart/dev/products');
  }

  /** Gọi 1 lần khi app khởi động (Header) để số trên biểu tượng giỏ đúng ngay. */
  refreshCount(): void {
    this.load().subscribe({ error: () => this.countSubject.next(0) });
  }

  private updateCount(cart: Cart): void {
    this.countSubject.next(cart.totalQuantity);
  }
}
