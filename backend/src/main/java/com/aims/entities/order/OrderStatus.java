package com.aims.entities.order;

public enum OrderStatus {
    PENDING_PAYMENT,     // đang nằm trong PendingOrderStore, chưa thanh toán
    PENDING_PROCESSING,  // đã thanh toán, chờ Product Manager duyệt
    APPROVED,
    REJECTED
}
