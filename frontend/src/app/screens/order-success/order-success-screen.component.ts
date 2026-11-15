import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderResult } from '../../models/cart-order.models';
import { OrderService } from '../../services/order.service';

/** Màn hình đặt hàng thành công: thông tin đơn + thông tin giao dịch. */
@Component({
  selector: 'app-order-success-screen',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  template: `
    <div class="page">
      @if (error) {
        <div class="alert">{{ error }}</div>
      } @else if (order) {
        <h2>✅ Đặt hàng thành công</h2>
        <p>Đơn hàng đang ở trạng thái <strong>chờ xử lý</strong>. Hóa đơn sẽ được gửi tới {{ order.email }}.</p>

        <h3>Thông tin đơn hàng #{{ order.orderId }}</h3>
        <table>
          <tr><th>Khách hàng</th><td>{{ order.customerName }}</td></tr>
          <tr><th>Số điện thoại</th><td>{{ order.phone }}</td></tr>
          <tr><th>Địa chỉ giao hàng</th><td>{{ order.address }}</td></tr>
          <tr><th>Tỉnh / Thành phố</th><td>{{ order.province }}</td></tr>
          <tr><th>Tổng số tiền</th><td><strong>{{ order.totalAmount | number: '1.0-0' }} đ</strong></td></tr>
        </table>

        <h3>Thông tin giao dịch</h3>
        @if (order.transactionId) {
          <table>
            <tr><th>Mã giao dịch</th><td>{{ order.transactionId }}</td></tr>
            <tr><th>Nội dung</th><td>{{ order.transactionContent }}</td></tr>
            <tr><th>Thời gian</th><td>{{ order.transactionTime }}</td></tr>
          </table>
        } @else {
          <p class="muted">Chưa có thông tin giao dịch (sẽ hiển thị khi phần thanh toán được ghép vào).</p>
        }

        <h3>Sản phẩm</h3>
        <ul>
          @for (i of order.items; track i.productId) {
            <li>{{ i.title }} × {{ i.quantity }} — {{ i.lineTotal | number: '1.0-0' }} đ</li>
          }
        </ul>
        <a routerLink="/" class="primary">Tiếp tục mua sắm</a>
      } @else {
        <p>Đang tải...</p>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 760px; margin: 24px auto; padding: 0 16px; }
    table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }
    th { text-align: left; width: 190px; padding: 6px 0; color: #555; }
    td { padding: 6px 0; }
    .muted { color: #666; }
    .alert { padding: 10px 12px; background: #fff4f4; border: 1px solid #ef9a9a; border-radius: 4px; }
    .primary { display: inline-block; margin-top: 16px; background: #1565c0; color: #fff; padding: 10px 24px; border-radius: 4px; text-decoration: none; }
  `],
})
export class OrderSuccessScreenComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);

  order: OrderResult | null = null;
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.orderService.getOrder(id).subscribe({
      next: (o) => (this.order = o),
      error: () => (this.error = 'Không tìm thấy đơn hàng.'),
    });
  }
}
