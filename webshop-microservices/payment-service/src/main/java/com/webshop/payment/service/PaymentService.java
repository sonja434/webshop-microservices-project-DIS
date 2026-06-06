package com.webshop.payment.service;

import com.webshop.payment.config.RabbitMQConfig;
import com.webshop.payment.dto.PaymentEvent;
import com.webshop.payment.dto.PaymentResponse;
import com.webshop.payment.model.Payment;
import com.webshop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    // Asinhrono prima poruku od order-service
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void processPayment(PaymentEvent event) {
        log.info("Received order event for orderId: {}", event.getOrderId());

        try {
            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getTotalAmount())
                    .status(Payment.PaymentStatus.COMPLETED)
                    .transactionId(UUID.randomUUID().toString())
                    .build();

            paymentRepository.save(payment);
            log.info("Payment completed for orderId: {}", event.getOrderId());

            // Asinhrono šalje poruku notification-service-u
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                    event);

        } catch (Exception e) {
            log.error("Payment failed for orderId: {}", event.getOrderId(), e);

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getTotalAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .build();

            paymentRepository.save(payment);
        }
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for order: " + orderId));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}