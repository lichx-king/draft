package com.aims.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Lỗi riêng của luồng giỏ hàng / đặt hàng.
 * TẠM THỜI: sau khi Đặng Đức Khải xong AimsException thì đổi sang dùng lớp của Khải.
 */
public class OrderFlowException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final transient Object details;

    public OrderFlowException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public OrderFlowException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Object getDetails() { return details; }
}
