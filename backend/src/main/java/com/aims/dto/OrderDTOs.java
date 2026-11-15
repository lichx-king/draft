package com.aims.dto;

import java.util.List;

public final class OrderDTOs {
    private OrderDTOs() {}

    public record DeliveryInfoRequest(String customerName, String phone, String email, String address,
                                      String province) {}

    public record ShippingFeeRequest(String province) {}

    public record ShippingFeeDTO(long shippingFee, boolean freeShippingApplied) {}

    public record StockShortageDTO(int productId, String title, int requested, int available) {}

    public record InvoiceLineDTO(int productId, String title, long unitPrice, int quantity, long lineTotal) {}

    public record DeliveryInfoDTO(String customerName, String phone, String email, String address,
                                  String province) {}

    /** Hóa đơn tạm hiển thị trước khi thanh toán. */
    public record InvoiceDTO(String pendingOrderId, List<InvoiceLineDTO> items, long totalExcludingVat,
                             long vatAmount, long totalIncludingVat, long shippingFee, long totalAmount,
                             DeliveryInfoDTO delivery) {}

    /** Thông tin hiển thị ở màn hình đặt hàng thành công. Phần giao dịch sẽ do bạn làm thanh toán bổ sung. */
    public record OrderResultDTO(long orderId, String status, String customerName, String phone, String email,
                                 String address, String province, long totalAmount, List<InvoiceLineDTO> items,
                                 String createdAt, String transactionId, String transactionContent,
                                 String transactionTime) {}
}
