import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiErrorBody, Cart, DevProduct, StockShortage } from '../../models/cart-order.models';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { CartItemRowComponent } from './cart-item-row.component';

/** UC06 - Màn hình giỏ hàng. */
@Component({
  selector: 'app-cart-screen',
  standalone: true,
  imports: [DecimalPipe, RouterLink, CartItemRowComponent],
  template: `
    <div class="page">
      <h2>Giỏ hàng</h2>

      @if (message) { <div class="alert">{{ message }}</div> }

      @if (shortages.length > 0) {
        <div class="alert error-box">
          <strong>Không đủ hàng để đặt. Vui lòng cập nhật giỏ hàng:</strong>
          <ul>
            @for (s of shortages; track s.productId) {
              <li>{{ s.title }}: bạn muốn {{ s.requested }}, chỉ còn {{ s.available }}.</li>
            }
          </ul>
        </div>
      }

      @if (cart && cart.items.length > 0) {
        @for (item of cart.items; track item.productId) {
          <app-cart-item-row
            [item]="item"
            (quantityChange)="changeQuantity(item.productId, $event)"
            (removed)="removeItem(item.productId)" />
        }

        <div class="summary">
          <div>Tổng tiền hàng (chưa VAT): <strong>{{ cart.totalExcludingVat | number: '1.0-0' }} đ</strong></div>
          <div class="muted">VAT 10% và phí giao hàng sẽ được tính ở bước sau.</div>
        </div>

        <div class="actions">
          <a routerLink="/">← Tiếp tục mua sắm</a>
          <button type="button" class="primary" (click)="placeOrder()" [disabled]="loading">Đặt hàng</button>
        </div>
      } @else if (cart) {
        <p>Giỏ hàng của bạn đang trống.</p>
        <a routerLink="/">← Về trang chủ</a>
      }

      <!-- Khung test: xóa khi màn hình danh sách sản phẩm của Thành đã có nút "Thêm vào giỏ" -->
      @if (DEV_MODE) {
        <div class="dev">
          <strong>[DEV] Thêm nhanh sản phẩm để thử giỏ hàng</strong>
          @for (p of devProducts; track p.id) {
            <div class="dev-row">
              <span>#{{ p.id }} {{ p.title }} — {{ p.price | number: '1.0-0' }} đ (tồn {{ p.stock }})</span>
              <button type="button" (click)="devAdd(p.id)">Thêm 1</button>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 900px; margin: 24px auto; padding: 0 16px; }
    .summary { text-align: right; padding: 16px 12px; font-size: 18px; }
    .muted { color: #666; font-size: 13px; }
    .actions { display: flex; justify-content: space-between; align-items: center; }
    .primary { background: #1565c0; color: #fff; border: 0; padding: 10px 28px; border-radius: 4px; cursor: pointer; font-size: 16px; }
    .primary:disabled { opacity: .6; cursor: default; }
    .alert { padding: 10px 12px; background: #fff8e1; border: 1px solid #ffe082; border-radius: 4px; margin-bottom: 12px; }
    .error-box { background: #fff4f4; border-color: #ef9a9a; color: #b71c1c; }
    .dev { margin-top: 32px; padding: 12px; border: 1px dashed #999; border-radius: 4px; }
    .dev-row { display: flex; justify-content: space-between; padding: 4px 0; }
  `],
})
export class CartScreenComponent implements OnInit {
  private cartService = inject(CartService);
  private orderService = inject(OrderService);
  private router = inject(Router);

  readonly DEV_MODE = true; // đặt false khi nộp bản cuối

  cart: Cart | null = null;
  shortages: StockShortage[] = [];
  devProducts: DevProduct[] = [];
  message = '';
  loading = false;

  ngOnInit(): void {
    this.reload();
    if (this.DEV_MODE) {
      this.cartService.devProducts().subscribe({ next: (p) => (this.devProducts = p), error: () => {} });
    }
  }

  reload(): void {
    this.cartService.load().subscribe({
      next: (c) => (this.cart = c),
      error: () => (this.message = 'Không tải được giỏ hàng. Hãy chắc chắn backend đang chạy và bạn mở web qua cổng 4200.'),
    });
  }

  changeQuantity(productId: number, quantity: number): void {
    this.shortages = [];
    this.cartService.updateQuantity(productId, quantity).subscribe({
      next: (c) => (this.cart = c),
      error: (e) => this.showError(e),
    });
  }

  removeItem(productId: number): void {
    this.shortages = [];
    this.cartService.remove(productId).subscribe({
      next: (c) => (this.cart = c),
      error: (e) => this.showError(e),
    });
  }

  devAdd(productId: number): void {
    this.cartService.add(productId, 1).subscribe({
      next: (c) => (this.cart = c),
      error: (e) => this.showError(e),
    });
  }

  /** Kiểm tra tồn kho rồi sang màn hình nhập thông tin giao hàng. */
  placeOrder(): void {
    this.loading = true;
    this.message = '';
    this.shortages = [];
    this.orderService.checkStock().subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/delivery']);
      },
      error: (e: HttpErrorResponse) => {
        this.loading = false;
        const body = e.error as ApiErrorBody | null;
        if (body?.code === 'INSUFFICIENT_STOCK') {
          this.shortages = body.details as StockShortage[];
          this.reload(); // cập nhật lại các dòng báo thiếu hàng
        } else {
          this.showError(e);
        }
      },
    });
  }

  private showError(e: HttpErrorResponse): void {
    this.message = (e.error as ApiErrorBody | null)?.message ?? 'Có lỗi xảy ra, vui lòng thử lại.';
  }
}
