package com.aims.entities.order;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** Đơn hàng. Tiền tính bằng VND (số nguyên). Chưa thanh toán -> nằm trong PendingOrderStore. */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final double VAT_RATE = 0.10;

    private Long id;                 // null cho tới khi recordOrder()
    private String pendingId;        // mã tạm dùng trước khi thanh toán
    private final DeliveryInfo delivery;
    private final List<OrderItem> items;
    private final long totalExcludingVat;
    private final long vatAmount;
    private final long totalIncludingVat;
    private final long shippingFee;
    private final long totalAmount;  // = totalIncludingVat + shippingFee
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Order(DeliveryInfo delivery, List<OrderItem> items, long shippingFee) {
        this.delivery = delivery;
        this.items = List.copyOf(items);
        this.totalExcludingVat = items.stream().mapToLong(OrderItem::lineTotal).sum();
        this.vatAmount = Math.round(totalExcludingVat * VAT_RATE);
        this.totalIncludingVat = totalExcludingVat + vatAmount;
        this.shippingFee = shippingFee;
        this.totalAmount = totalIncludingVat + shippingFee;
    }

    /** Dùng khi đọc lại từ DB. */
    public Order(Long id, DeliveryInfo delivery, List<OrderItem> items, long shippingFee,
                 OrderStatus status, LocalDateTime createdAt) {
        this(delivery, items, shippingFee);
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPendingId() { return pendingId; }
    public void setPendingId(String pendingId) { this.pendingId = pendingId; }
    public DeliveryInfo getDelivery() { return delivery; }
    public List<OrderItem> getItems() { return items; }
    public long getTotalExcludingVat() { return totalExcludingVat; }
    public long getVatAmount() { return vatAmount; }
    public long getTotalIncludingVat() { return totalIncludingVat; }
    public long getShippingFee() { return shippingFee; }
    public long getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
