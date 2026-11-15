package com.aims.entities.cart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Giỏ hàng lưu trong HttpSession: chỉ giữ productId -> số lượng. Giá/tồn kho luôn đọc mới từ DB. */
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, Integer> items = new LinkedHashMap<>();

    public synchronized void add(int productId, int quantity) {
        items.merge(productId, quantity, Integer::sum);
    }

    public synchronized void setQuantity(int productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            items.put(productId, quantity);
        }
    }

    public synchronized void remove(int productId) { items.remove(productId); }

    public synchronized void clear() { items.clear(); }

    public synchronized boolean isEmpty() { return items.isEmpty(); }

    public synchronized Map<Integer, Integer> snapshot() { return new LinkedHashMap<>(items); }
}
