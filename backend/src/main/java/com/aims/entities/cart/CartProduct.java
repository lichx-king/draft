package com.aims.entities.cart;

/** Ảnh chụp thông tin sản phẩm mà giỏ hàng cần (giá chưa VAT, cân nặng kg, tồn kho). */
public record CartProduct(int id, String title, String imageUrl, long price, double weightKg, int stock) {
}
