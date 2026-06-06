package com.webshop.payment.controller;

import com.webshop.payment.dto.PaymentResponse;
import com.webshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
                paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUserId(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByUserId(userId));
    }
}