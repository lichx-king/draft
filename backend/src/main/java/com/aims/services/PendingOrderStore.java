package com.aims.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.aims.entities.order.Order;

/**
 * Lưu tạm hóa đơn/đơn hàng trong bộ nhớ cho tới khi thanh toán xong ("temporarily save invoice").
 * Mất khi tắt backend - chấp nhận được cho bản demo. Đơn tạm quá 2 giờ sẽ bị dọn.
 */
@Component
public class PendingOrderStore {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public String save(Order order) {
        purgeExpired();
        String id = UUID.randomUUID().toString();
        order.setPendingId(id);
        store.put(id, order);
        return id;
    }

    public Optional<Order> find(String pendingId) {
        return Optional.ofNullable(pendingId == null ? null : store.get(pendingId));
    }

    public void remove(String pendingId) {
        if (pendingId != null) {
            store.remove(pendingId);
        }
    }

    private void purgeExpired() {
        LocalDateTime limit = LocalDateTime.now().minusHours(2);
        store.values().removeIf(o -> o.getCreatedAt().isBefore(limit));
    }
}
