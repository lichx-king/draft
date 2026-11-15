import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  ApiErrorBody, Cart, DeliveryInfo, Invoice, PROVINCES, StockShortage,
} from '../../models/cart-order.models';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

/** UC08 - Nhập thông tin giao hàng, xem phí giao hàng, tạo hóa đơn tạm. */
@Component({
  selector: 'app-delivery-info-screen',
  standalone: true,
  imports: [DecimalPipe, FormsModule, RouterLink],
  template: `
    <div class="page">
      <h2>Thông tin giao hàng</h2>
      @if (message) { <div class="alert">{{ message }}</div> }
      @if (shortages.length > 0) {
        <div class="alert error-box">
          Không đủ hàng, vui lòng <a routerLink="/cart">quay lại giỏ hàng</a> để cập nhật:
          <ul>
            @for (s of shortages; track s.productId) {
              <li>{{ s.title }}: bạn muốn {{ s.requested }}, chỉ còn {{ s.available }}.</li>
            }
          </ul>
        </div>
      }

      <div class="layout">
        <!-- Cột trái: biểu mẫu hoặc hóa đơn tạm -->
        <div class="left">
          @if (!invoice) {
            <form (ngSubmit)="submit()" novalidate>
              <label>Họ và tên *
                <input name="customerName" [(ngModel)]="info.customerName" />
                @if (errors['customerName']) { <small class="err">{{ errors['customerName'] }}</small> }
              </label>
              <label>Số điện thoại *
                <input name="phone" [(ngModel)]="info.phone" />
                @if (errors['phone']) { <small class="err">{{ errors['phone'] }}</small> }
              </label>
              <label>Email *
                <input name="email" [(ngModel)]="info.email" />
                @if (errors['email']) { <small class="err">{{ errors['email'] }}</small> }
              </label>
              <label>Tỉnh / Thành phố *
                <select name="province" [(ngModel)]="info.province" (ngModelChange)="recalcFee()">
                  <option value="">-- Chọn --</option>
                  @for (p of provinces; track p) { <option [value]="p">{{ p }}</option> }
                </select>
                @if (errors['province']) { <small class="err">{{ errors['province'] }}</small> }
              </label>
              <label>Địa chỉ giao hàng *
                <input name="address" [(ngModel)]="info.address" (blur)="recalcFee()" />
                @if (errors['address']) { <small class="err">{{ errors['address'] }}</small> }
              </label>
              <div class="actions">
                <a routerLink="/cart">← Quay lại giỏ hàng</a>
                <button type="submit" class="primary" [disabled]="loading">Tiếp tục thanh toán</button>
              </div>
            </form>
          } @else {
            <h3>Hóa đơn</h3>
            <p>
              <strong>{{ invoice.delivery.customerName }}</strong> — {{ invoice.delivery.phone }}<br />
              {{ invoice.delivery.email }}<br />
              {{ invoice.delivery.address }}, {{ invoice.delivery.province }}
            </p>
            <div class="actions">
              <button type="button" (click)="editInfo()">← Sửa thông tin</button>
            </div>
            <div class="pay-box">
              <button type="button" class="primary" (click)="goToPayment()">Thanh toán (VietQR / PayPal)</button>
              @if (DEV_PAYMENT) {
                <button type="button" class="dev" (click)="devPay()" [disabled]="loading">
                  [DEV] Giả lập thanh toán thành công
                </button>
              }
            </div>
          }
        </div>

        <!-- Cột phải: sản phẩm trong giỏ và tổng tiền -->
        <div class="right">
          <h3>Đơn hàng</h3>
          @if (invoice) {
            @for (l of invoice.items; track l.productId) {
              <div class="line"><span>{{ l.title }} × {{ l.quantity }}</span><span>{{ l.lineTotal | number: '1.0-0' }} đ</span></div>
            }
            <div class="line"><span>Tiền hàng (chưa VAT)</span><span>{{ invoice.totalExcludingVat | number: '1.0-0' }} đ</span></div>
            <div class="line"><span>VAT 10%</span><span>{{ invoice.vatAmount | number: '1.0-0' }} đ</span></div>
            <div class="line"><span>Tiền hàng (đã VAT)</span><span>{{ invoice.totalIncludingVat | number: '1.0-0' }} đ</span></div>
            <div class="line"><span>Phí giao hàng</span><span>{{ invoice.shippingFee | number: '1.0-0' }} đ</span></div>
            <div class="line total"><span>Tổng thanh toán</span><span>{{ invoice.totalAmount | number: '1.0-0' }} đ</span></div>
          } @else if (cart) {
            @for (i of cart.items; track i.productId) {
              <div class="line"><span>{{ i.title }} × {{ i.quantity }}</span><span>{{ i.lineTotal | number: '1.0-0' }} đ</span></div>
            }
            <div class="line"><span>Tiền hàng (chưa VAT)</span><span>{{ cart.totalExcludingVat | number: '1.0-0' }} đ</span></div>
            <div class="line"><span>VAT 10%</span><span>{{ vat | number: '1.0-0' }} đ</span></div>
            <div class="line"><span>Phí giao hàng</span>
              <span>@if (shippingFee === null) { (chọn tỉnh/thành để tính) } @else { {{ shippingFee | number: '1.0-0' }} đ }</span>
            </div>
            @if (freeShip) { <div class="muted">Đã áp dụng giảm phí giao hàng (đơn trên 200.000 đ).</div> }
            <div class="line total"><span>Tạm tính</span><span>{{ estimatedTotal | number: '1.0-0' }} đ</span></div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { max-width: 980px; margin: 24px auto; padding: 0 16px; }
    .layout { display: flex; gap: 32px; flex-wrap: wrap; }
    .left { flex: 1 1 380px; } .right { flex: 1 1 320px; background: #fafafa; padding: 12px 16px; border-radius: 6px; }
    label { display: block; margin-bottom: 12px; font-weight: 600; }
    input, select { display: block; width: 100%; box-sizing: border-box; padding: 8px; margin-top: 4px; font-weight: 400; }
    .err { color: #c62828; font-weight: 400; }
    .line { display: flex; justify-content: space-between; padding: 6px 0; gap: 12px; }
    .line.total { border-top: 1px solid #ccc; font-weight: 700; font-size: 18px; margin-top: 6px; }
    .muted { color: #2e7d32; font-size: 13px; }
    .actions { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
    .primary { background: #1565c0; color: #fff; border: 0; padding: 10px 24px; border-radius: 4px; cursor: pointer; font-size: 16px; }
    .primary:disabled { opacity: .6; }
    .pay-box { display: flex; flex-direction: column; gap: 10px; margin-top: 20px; }
    .dev { padding: 8px; border: 1px dashed #999; background: #fff; cursor: pointer; }
    .alert { padding: 10px 12px; background: #fff8e1; border: 1px solid #ffe082; border-radius: 4px; margin-bottom: 12px; }
    .error-box { background: #fff4f4; border-color: #ef9a9a; color: #b71c1c; }
  `],
})
export class DeliveryInfoScreenComponent implements OnInit {
  private cartService = inject(CartService);
  private orderService = inject(OrderService);
  private router = inject(Router);

  readonly provinces = PROVINCES;
  readonly DEV_PAYMENT = true; // true: hiện nút giả lập thanh toán. Đặt false khi đã nối VietQR/PayPal.
  readonly PAYMENT_ROUTE = '/payment'; // đường dẫn màn hình thanh toán của Lê Quý Thành (sửa nếu khác)

  cart: Cart | null = null;
  info: DeliveryInfo = { customerName: '', phone: '', email: '', address: '', province: '' };
  errors: Record<string, string> = {};
  shortages: StockShortage[] = [];
  invoice: Invoice | null = null;
  shippingFee: number | null = null;
  freeShip = false;
  message = '';
  loading = false;

  ngOnInit(): void {
    this.cartService.load().subscribe({
      next: (c) => {
        if (c.items.length === 0) {
          this.router.navigate(['/cart']);
        }
        this.cart = c;
      },
      error: () => (this.message = 'Không tải được giỏ hàng.'),
    });
  }

  get vat(): number {
    return Math.round((this.cart?.totalExcludingVat ?? 0) * 0.1);
  }

  get estimatedTotal(): number {
    return (this.cart?.totalExcludingVat ?? 0) + this.vat + (this.shippingFee ?? 0);
  }

  /** Tính lại phí giao hàng mỗi khi khách đổi tỉnh hoặc địa chỉ. */
  recalcFee(): void {
    if (!this.info.province) {
      this.shippingFee = null;
      return;
    }
    this.orderService.getShippingFee(this.info.province).subscribe({
      next: (r) => {
        this.shippingFee = r.shippingFee;
        this.freeShip = r.freeShippingApplied;
      },
      error: () => (this.shippingFee = null),
    });
  }

  submit(): void {
    this.loading = true;
    this.message = '';
    this.errors = {};
    this.shortages = [];
    this.orderService.submitDeliveryInfo(this.info).subscribe({
      next: (inv) => {
        this.loading = false;
        this.invoice = inv;
      },
      error: (e: HttpErrorResponse) => {
        this.loading = false;
        const body = e.error as ApiErrorBody | null;
        if (body?.code === 'VALIDATION_ERROR') {
          this.errors = body.details as Record<string, string>;
        } else if (body?.code === 'INSUFFICIENT_STOCK') {
          this.shortages = body.details as StockShortage[];
        } else {
          this.message = body?.message ?? 'Có lỗi xảy ra, vui lòng thử lại.';
        }
      },
    });
  }

  editInfo(): void {
    this.invoice = null; // quay lại biểu mẫu, thông tin đã nhập vẫn còn
  }

  goToPayment(): void {
    this.router.navigate([this.PAYMENT_ROUTE, this.invoice!.pendingOrderId]);
  }

  devPay(): void {
    this.loading = true;
    this.orderService.devConfirm(this.invoice!.pendingOrderId).subscribe({
      next: (order) => {
        this.cartService.refreshCount();
        this.router.navigate(['/order-success', order.orderId]);
      },
      error: (e: HttpErrorResponse) => {
        this.loading = false;
        this.message = (e.error as ApiErrorBody | null)?.message ?? 'Có lỗi xảy ra.';
      },
    });
  }
}
