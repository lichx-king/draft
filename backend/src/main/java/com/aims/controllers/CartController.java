package com.aims.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aims.dao.CartProductDAO;
import com.aims.dto.CartDTOs.AddItemRequest;
import com.aims.dto.CartDTOs.CartDTO;
import com.aims.dto.CartDTOs.DevProductDTO;
import com.aims.dto.CartDTOs.UpdateQuantityRequest;
import com.aims.services.CartService;

import jakarta.servlet.http.HttpSession;

/** UC06 - Xem / sửa giỏ hàng. Mọi thao tác trả về giỏ hàng mới nhất. */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CartProductDAO productDAO;

    public CartController(CartService cartService, CartProductDAO productDAO) {
        this.cartService = cartService;
        this.productDAO = productDAO;
    }

    @GetMapping
    public CartDTO viewCart(HttpSession session) {
        return current(session);
    }

    @PostMapping("/items")
    public CartDTO addItem(@RequestBody AddItemRequest req, HttpSession session) {
        cartService.addItem(session, req.productId(), req.quantity());
        return current(session);
    }

    @PutMapping("/items/{productId}")
    public CartDTO updateQuantity(@PathVariable int productId, @RequestBody UpdateQuantityRequest req,
                                  HttpSession session) {
        cartService.updateQuantity(session, productId, req.quantity());
        return current(session);
    }

    @DeleteMapping("/items/{productId}")
    public CartDTO removeItem(@PathVariable int productId, HttpSession session) {
        cartService.removeItem(session, productId);
        return current(session);
    }

    /** CHỈ ĐỂ TEST: liệt kê vài sản phẩm để thêm vào giỏ khi chưa có màn hình của Thành. Xóa khi ghép xong. */
    @GetMapping("/dev/products")
    public List<DevProductDTO> devProducts() {
        return productDAO.findSome(10).stream()
                .map(p -> new DevProductDTO(p.id(), p.title(), p.price(), p.stock())).toList();
    }

    private CartDTO current(HttpSession session) {
        return cartService.toDTO(cartService.loadItems(session));
    }
}
