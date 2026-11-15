package com.aims.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.aims.dao.CartProductDAO;
import com.aims.dto.CartDTOs.CartDTO;
import com.aims.dto.CartDTOs.CartItemDTO;
import com.aims.entities.cart.Cart;
import com.aims.entities.cart.CartItem;
import com.aims.entities.cart.CartProduct;
import com.aims.exceptions.OrderFlowException;

import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    private static final String SESSION_KEY = "AIMS_CART";

    private final CartProductDAO productDAO;

    public CartService(CartProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    /** Lấy giỏ của phiên hiện tại (tạo mới nếu chưa có). */
    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(SESSION_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(SESSION_KEY, cart);
        }
        return cart;
    }

    public void addItem(HttpSession session, int productId, int quantity) {
        if (quantity <= 0) {
            throw new OrderFlowException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "Số lượng phải lớn hơn 0.");
        }
        productDAO.findById(productId).orElseThrow(() ->
                new OrderFlowException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm."));
        getCart(session).add(productId, quantity);
    }

    public void updateQuantity(HttpSession session, int productId, int quantity) {
        if (quantity < 0) {
            throw new OrderFlowException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "Số lượng không hợp lệ.");
        }
        getCart(session).setQuantity(productId, quantity); // quantity = 0 nghĩa là xóa
    }

    public void removeItem(HttpSession session, int productId) {
        getCart(session).remove(productId);
    }

    public void clear(HttpSession session) {
        getCart(session).clear();
    }

    /** Các dòng trong giỏ với giá/tồn kho mới nhất. Sản phẩm đã bị xóa khỏi DB sẽ tự bị bỏ khỏi giỏ. */
    public List<CartItem> loadItems(HttpSession session) {
        Cart cart = getCart(session);
        List<CartItem> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cart.snapshot().entrySet()) {
            CartProduct p = productDAO.findById(e.getKey()).orElse(null);
            if (p == null) {
                cart.remove(e.getKey());
            } else {
                result.add(new CartItem(p, e.getValue()));
            }
        }
        return result;
    }

    public CartDTO toDTO(List<CartItem> items) {
        List<CartItemDTO> dtos = items.stream().map(i -> new CartItemDTO(
                i.product().id(), i.product().title(), i.product().imageUrl(), i.product().price(),
                i.quantity(), i.product().stock(), i.insufficient(), i.missingQuantity(), i.lineTotal()))
                .toList();
        int totalQty = items.stream().mapToInt(CartItem::quantity).sum();
        long total = items.stream().mapToLong(CartItem::lineTotal).sum();
        boolean anyShort = items.stream().anyMatch(CartItem::insufficient);
        return new CartDTO(dtos, totalQty, total, anyShort);
    }
}
