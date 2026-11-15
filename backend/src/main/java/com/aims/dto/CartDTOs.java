package com.aims.dto;

import java.util.List;

public final class CartDTOs {
    private CartDTOs() {}

    public record CartItemDTO(int productId, String title, String imageUrl, long unitPrice, int quantity,
                              int availableStock, boolean insufficient, int missingQuantity, long lineTotal) {}

    public record CartDTO(List<CartItemDTO> items, int totalQuantity, long totalExcludingVat,
                          boolean hasInsufficientStock) {}

    public record AddItemRequest(int productId, int quantity) {}

    public record UpdateQuantityRequest(int quantity) {}

    /** Chỉ dùng để test khi chưa có màn hình danh sách sản phẩm của Thành. */
    public record DevProductDTO(int id, String title, long price, int stock) {}
}
