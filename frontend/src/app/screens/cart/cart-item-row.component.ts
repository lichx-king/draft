import { DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CartItem } from '../../models/cart-order.models';

@Component({
  selector: 'app-cart-item-row',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <div class="row" [class.warn]="item.insufficient">
      <div class="info">
        <strong>{{ item.title }}</strong>
        <div class="muted">Đơn giá: {{ item.unitPrice | number: '1.0-0' }} đ (chưa VAT)</div>
        @if (item.insufficient) {
          <div class="error">
            Không đủ hàng: còn {{ item.availableStock }}, thiếu {{ item.missingQuantity }}.
          </div>
        }
      </div>

      <div class="qty">
        <button type="button" (click)="change(item.quantity - 1)" [disabled]="item.quantity <= 1">−</button>
        <input type="number" min="1" [value]="item.quantity" (change)="onInput($event)" />
        <button type="button" (click)="change(item.quantity + 1)">+</button>
      </div>

      <div class="total">{{ item.lineTotal | number: '1.0-0' }} đ</div>
      <button type="button" class="remove" (click)="removed.emit()">Xóa</button>
    </div>
  `,
  styles: [`
    .row { display: flex; align-items: center; gap: 16px; padding: 12px; border-bottom: 1px solid #ddd; }
    .row.warn { background: #fff4f4; }
    .info { flex: 1; }
    .muted { color: #666; font-size: 13px; }
    .error { color: #c62828; font-size: 13px; margin-top: 4px; }
    .qty { display: flex; align-items: center; gap: 4px; }
    .qty input { width: 56px; text-align: center; padding: 4px; }
    .qty button { width: 28px; height: 28px; cursor: pointer; }
    .total { width: 130px; text-align: right; font-weight: 600; }
    .remove { cursor: pointer; color: #c62828; background: none; border: 1px solid #c62828; border-radius: 4px; padding: 4px 10px; }
  `],
})
export class CartItemRowComponent {
  @Input({ required: true }) item!: CartItem;
  /** Phát ra số lượng mới khi khách bấm -, + hoặc gõ số. */
  @Output() quantityChange = new EventEmitter<number>();
  @Output() removed = new EventEmitter<void>();

  change(q: number): void {
    if (q >= 1) {
      this.quantityChange.emit(q);
    }
  }

  onInput(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (Number.isInteger(value) && value >= 1) {
      this.quantityChange.emit(value);
    } else {
      (event.target as HTMLInputElement).value = String(this.item.quantity); // trả về số cũ
    }
  }
}
