package com.aims.entities.order;

import java.io.Serializable;

public record OrderItem(int productId, String title, long unitPrice, int quantity) implements Serializable {
    public long lineTotal() { return unitPrice * quantity; }
}
