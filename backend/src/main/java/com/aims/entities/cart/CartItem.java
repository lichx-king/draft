package com.aims.entities.cart;

/** Một dòng trong giỏ: sản phẩm + số lượng khách muốn mua. */
public record CartItem(CartProduct product, int quantity) {

    public long lineTotal() { return product.price() * quantity; }

    public double lineWeightKg() { return product.weightKg() * quantity; }

    public boolean insufficient() { return quantity > product.stock(); }

    /** Số lượng còn thiếu so với tồn kho (0 nếu đủ). */
    public int missingQuantity() { return Math.max(0, quantity - Math.max(0, product.stock())); }
}
