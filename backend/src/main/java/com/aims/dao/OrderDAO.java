package com.aims.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aims.entities.order.DeliveryInfo;
import com.aims.entities.order.Order;
import com.aims.entities.order.OrderItem;
import com.aims.entities.order.OrderStatus;
import com.aims.services.PendingOrderStore;

/**
 * Hai hàm các bạn làm thanh toán sẽ gọi:
 *   findPendingOrder(pendingId)  - lấy đơn tạm (hóa đơn) trước khi thanh toán
 *   recordOrder(order)           - ghi đơn đã thanh toán thành công vào DB
 * Bảng dùng tạm: place_order, place_order_item (xem database/hung_order.sql).
 */
@Repository
public class OrderDAO {

    private final JdbcTemplate jdbc;
    private final PendingOrderStore pendingStore;

    public OrderDAO(JdbcTemplate jdbc, PendingOrderStore pendingStore) {
        this.jdbc = jdbc;
        this.pendingStore = pendingStore;
    }

    public Optional<Order> findPendingOrder(String pendingId) {
        return pendingStore.find(pendingId);
    }

    @Transactional
    public Order recordOrder(Order order) {
        order.setStatus(OrderStatus.PENDING_PROCESSING);
        KeyHolder keys = new GeneratedKeyHolder();
        DeliveryInfo d = order.getDelivery();

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO place_order (customer_name, phone, email, address, province, "
                            + "total_excluding_vat, vat_amount, total_including_vat, shipping_fee, total_amount, "
                            + "status, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, d.customerName());
            ps.setString(i++, d.phone());
            ps.setString(i++, d.email());
            ps.setString(i++, d.address());
            ps.setString(i++, d.province());
            ps.setLong(i++, order.getTotalExcludingVat());
            ps.setLong(i++, order.getVatAmount());
            ps.setLong(i++, order.getTotalIncludingVat());
            ps.setLong(i++, order.getShippingFee());
            ps.setLong(i++, order.getTotalAmount());
            ps.setString(i++, order.getStatus().name());
            ps.setTimestamp(i, Timestamp.valueOf(order.getCreatedAt()));
            return ps;
        }, keys);
        long orderId = keys.getKey().longValue();
        order.setId(orderId);

        for (OrderItem item : order.getItems()) {
            jdbc.update("INSERT INTO place_order_item (order_id, product_id, title, unit_price, quantity) "
                    + "VALUES (?,?,?,?,?)", orderId, item.productId(), item.title(), item.unitPrice(),
                    item.quantity());
            // Tồn kho tự giảm khi bán (không để âm)
            jdbc.update("UPDATE product SET quantity = GREATEST(quantity - ?, 0) WHERE id = ?",
                    item.quantity(), item.productId());
        }

        if (order.getPendingId() != null) {
            pendingStore.remove(order.getPendingId());
        }
        return order;
    }

    public Optional<Order> findById(long orderId) {
        List<Order> rows = jdbc.query("SELECT * FROM place_order WHERE id = ?", (rs, i) -> {
            DeliveryInfo d = new DeliveryInfo(rs.getString("customer_name"), rs.getString("phone"),
                    rs.getString("email"), rs.getString("address"), rs.getString("province"));
            List<OrderItem> items = jdbc.query(
                    "SELECT product_id, title, unit_price, quantity FROM place_order_item WHERE order_id = ?",
                    (r2, j) -> new OrderItem(r2.getInt("product_id"), r2.getString("title"),
                            r2.getLong("unit_price"), r2.getInt("quantity")),
                    orderId);
            return new Order(rs.getLong("id"), d, items, rs.getLong("shipping_fee"),
                    OrderStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime());
        }, orderId);
        return rows.stream().findFirst();
    }
}
