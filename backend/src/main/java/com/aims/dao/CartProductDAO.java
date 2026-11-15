package com.aims.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.aims.entities.cart.CartProduct;

/**
 * Đọc thông tin sản phẩm mà giỏ hàng cần.
 * Giả định bảng `product` có các cột: id, title, current_price, weight, quantity, image_url.
 * Nếu bảng thật của nhóm đặt tên cột khác, chỉ cần sửa SELECT và MAPPER bên dưới.
 * Khi Đoàn Tiến Thành xong ProductDAO.findProductById() thì có thể gọi hàm đó thay cho lớp này.
 */
@Repository
public class CartProductDAO {

    private static final String SELECT =
            "SELECT id, title, current_price, weight, quantity, image_url FROM product";

    private static final RowMapper<CartProduct> MAPPER = (rs, i) -> new CartProduct(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("image_url"),
            rs.getLong("current_price"),
            rs.getDouble("weight"),
            rs.getInt("quantity"));

    private final JdbcTemplate jdbc;

    public CartProductDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CartProduct> findById(int id) {
        List<CartProduct> rows = jdbc.query(SELECT + " WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    /** Chỉ dùng cho khung test trong màn hình giỏ hàng. */
    public List<CartProduct> findSome(int limit) {
        return jdbc.query(SELECT + " ORDER BY id LIMIT ?", MAPPER, limit);
    }
}
