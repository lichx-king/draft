package com.aims.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Chỉ bắt OrderFlowException, không đụng tới ApiExceptionHandler của Khải. */
@RestControllerAdvice
public class OrderFlowExceptionHandler {

    @ExceptionHandler(OrderFlowException.class)
    public ResponseEntity<Map<String, Object>> handle(OrderFlowException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("details", ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
