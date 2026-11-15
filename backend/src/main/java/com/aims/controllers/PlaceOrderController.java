package com.aims.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aims.dao.OrderDAO;
import com.aims.dto.OrderDTOs.DeliveryInfoDTO;
import com.aims.dto.OrderDTOs.DeliveryInfoRequest;
import com.aims.dto.OrderDTOs.InvoiceDTO;
import com.aims.dto.OrderDTOs.InvoiceLineDTO;
import com.aims.dto.OrderDTOs.OrderResultDTO;
import com.aims.dto.OrderDTOs.ShippingFeeDTO;
import com.aims.dto.OrderDTOs.ShippingFeeRequest;
import com.aims.dto.OrderDTOs.StockShortageDTO;
import com.aims.entities.cart.CartItem;
import com.aims.entities.order.DeliveryInfo;
import com.aims.entities.order.Order;
import com.aims.entities.order.OrderItem;
import com.aims.exceptions.OrderFlowException;
import com.aims.services.CartService;
import com.aims.services.PendingOrderStore;
import com.aims.services.ShippingFeeCalculator;

import jakarta.servlet.http.HttpSession;

/** UC08 - Place Order: kiểm tra tồn kho -> nhập thông tin giao hàng -> tạo hóa đơn tạm. */
@RestController
@RequestMapping("/api/orders")
public class PlaceOrderController {

    private static final String SESSION_PENDING_ID = "AIMS_PENDING_ORDER_ID";
    private static final Pattern PHONE = Pattern.compile("^(0|\\+84)\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final CartService cartService;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final PendingOrderStore pendingStore;
    private final OrderDAO orderDAO;

    public PlaceOrderController(CartService cartService, ShippingFeeCalculator shippingFeeCalculator,
                                PendingOrderStore pendingStore, OrderDAO orderDAO) {
        this.cartService = cartService;
        this.shippingFeeCalculator = shippingFeeCalculator;
        this.pendingStore = pendingStore;
        this.orderDAO = orderDAO;
    }

    /** Bước 1: bấm "Đặt hàng" ở giỏ hàng. Thiếu hàng -> 409 kèm số lượng còn lại. */
    @PostMapping("/check-stock")
    public Map<String, Object> checkStock(HttpSession session) {
        requireStockOk(cartService.loadItems(session));
        return Map.of("ok", true);
    }

    /** Tính (lại) phí giao hàng mỗi khi khách đổi tỉnh/địa chỉ. */
    @PostMapping("/shipping-fee")
    public ShippingFeeDTO shippingFee(@RequestBody ShippingFeeRequest req, HttpSession session) {
        List<CartItem> items = cartService.loadItems(session);
        long itemsTotal = items.stream().mapToLong(CartItem::lineTotal).sum();
        double weight = items.stream().mapToDouble(CartItem::lineWeightKg).sum();
        long fee = shippingFeeCalculator.calculate(req.province(), weight, itemsTotal);
        return new ShippingFeeDTO(fee, itemsTotal > ShippingFeeCalculator.FREE_SHIP_THRESHOLD);
    }

    /** Bước 2: gửi thông tin giao hàng -> kiểm tra -> tạo hóa đơn tạm, chuyển sang thanh toán. */
    @PostMapping("/delivery-info")
    public InvoiceDTO submitDeliveryInfo(@RequestBody DeliveryInfoRequest req, HttpSession session) {
        validate(req);

        List<CartItem> cartItems = cartService.loadItems(session);
        if (cartItems.isEmpty()) {
            throw new OrderFlowException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "Giỏ hàng đang trống.");
        }
        requireStockOk(cartItems);

        DeliveryInfo delivery = new DeliveryInfo(req.customerName().trim(), req.phone().trim(),
                req.email().trim(), req.address().trim(), req.province().trim());
        List<OrderItem> items = cartItems.stream().map(i -> new OrderItem(
                i.product().id(), i.product().title(), i.product().price(), i.quantity())).toList();
        long itemsTotal = cartItems.stream().mapToLong(CartItem::lineTotal).sum();
        double weight = cartItems.stream().mapToDouble(CartItem::lineWeightKg).sum();
        long fee = shippingFeeCalculator.calculate(delivery.province(), weight, itemsTotal);

        Order order = new Order(delivery, items, fee);

        // Đặt lại nhiều lần thì bỏ hóa đơn tạm cũ
        pendingStore.remove((String) session.getAttribute(SESSION_PENDING_ID));
        pendingStore.save(order);
        session.setAttribute(SESSION_PENDING_ID, order.getPendingId());

        return toInvoice(order);
    }

    /** Màn hình thanh toán (VietQR / PayPal) gọi để lấy hóa đơn tạm. */
    @GetMapping("/pending/{pendingId}")
    public InvoiceDTO getPending(@PathVariable String pendingId) {
        return toInvoice(orderDAO.findPendingOrder(pendingId).orElseThrow(() -> new OrderFlowException(
                HttpStatus.NOT_FOUND, "PENDING_ORDER_NOT_FOUND", "Không tìm thấy đơn hàng đang chờ thanh toán.")));
    }

    /**
     * CHỈ ĐỂ DEMO khi chưa có thanh toán thật: coi như đã thanh toán, ghi đơn và xóa giỏ.
     * Khi ghép xong, controller thanh toán của Thành/Khải gọi orderDAO.recordOrder(order)
     * và cartService.clear(session) thay cho endpoint này, rồi xóa endpoint này đi.
     */
    @PostMapping("/pending/{pendingId}/dev-confirm")
    public OrderResultDTO devConfirm(@PathVariable String pendingId, HttpSession session) {
        Order pending = orderDAO.findPendingOrder(pendingId).orElseThrow(() -> new OrderFlowException(
                HttpStatus.NOT_FOUND, "PENDING_ORDER_NOT_FOUND", "Không tìm thấy đơn hàng đang chờ thanh toán."));
        Order saved = orderDAO.recordOrder(pending);
        cartService.clear(session);
        session.removeAttribute(SESSION_PENDING_ID);
        return toResult(saved);
    }

    /** Màn hình "đặt hàng thành công". */
    @GetMapping("/{orderId}")
    public OrderResultDTO getOrder(@PathVariable long orderId) {
        return toResult(orderDAO.findById(orderId).orElseThrow(() -> new OrderFlowException(
                HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng.")));
    }

    // ---------------- helpers ----------------

    private void requireStockOk(List<CartItem> items) {
        if (items.isEmpty()) {
            throw new OrderFlowException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "Giỏ hàng đang trống.");
        }
        List<StockShortageDTO> shortages = items.stream().filter(CartItem::insufficient)
                .map(i -> new StockShortageDTO(i.product().id(), i.product().title(), i.quantity(),
                        Math.max(0, i.product().stock())))
                .toList();
        if (!shortages.isEmpty()) {
            throw new OrderFlowException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Một số sản phẩm không đủ hàng. Vui lòng cập nhật giỏ hàng.", shortages);
        }
    }

    private void validate(DeliveryInfoRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (blank(r.customerName())) errors.put("customerName", "Vui lòng nhập họ tên.");
        if (blank(r.phone())) errors.put("phone", "Vui lòng nhập số điện thoại.");
        else if (!PHONE.matcher(r.phone().trim()).matches())
            errors.put("phone", "Số điện thoại không hợp lệ (10 số bắt đầu bằng 0, hoặc +84...).");
        if (blank(r.email())) errors.put("email", "Vui lòng nhập email.");
        else if (!EMAIL.matcher(r.email().trim()).matches()) errors.put("email", "Email không hợp lệ.");
        if (blank(r.address())) errors.put("address", "Vui lòng nhập địa chỉ giao hàng.");
        if (blank(r.province())) errors.put("province", "Vui lòng chọn tỉnh/thành phố.");
        if (!errors.isEmpty()) {
            throw new OrderFlowException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Thông tin giao hàng chưa hợp lệ.", errors);
        }
    }

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    private List<InvoiceLineDTO> lines(Order o) {
        return o.getItems().stream().map(i -> new InvoiceLineDTO(i.productId(), i.title(), i.unitPrice(),
                i.quantity(), i.lineTotal())).toList();
    }

    private InvoiceDTO toInvoice(Order o) {
        DeliveryInfo d = o.getDelivery();
        return new InvoiceDTO(o.getPendingId(), lines(o), o.getTotalExcludingVat(), o.getVatAmount(),
                o.getTotalIncludingVat(), o.getShippingFee(), o.getTotalAmount(),
                new DeliveryInfoDTO(d.customerName(), d.phone(), d.email(), d.address(), d.province()));
    }

    private OrderResultDTO toResult(Order o) {
        DeliveryInfo d = o.getDelivery();
        // 3 trường giao dịch để null: bạn làm thanh toán sẽ bổ sung khi có PaymentTransactionDAO
        return new OrderResultDTO(o.getId(), o.getStatus().name(), d.customerName(), d.phone(), d.email(),
                d.address(), d.province(), o.getTotalAmount(), lines(o), o.getCreatedAt().toString(),
                null, null, null);
    }
}
