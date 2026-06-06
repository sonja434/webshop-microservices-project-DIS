package com.webshop.payment;

import com.webshop.payment.dto.PaymentEvent;
import com.webshop.payment.dto.PaymentResponse;
import com.webshop.payment.model.Payment;
import com.webshop.payment.repository.PaymentRepository;
import com.webshop.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_ShouldCreateCompletedPayment_WhenValidEvent() {
        PaymentEvent event = PaymentEvent.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(199.99))
                .status("PENDING")
                .shippingAddress("123 Main St")
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(1L);
                    return payment;
                });

        paymentService.processPayment(event);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(rabbitTemplate, times(1)).convertAndSend(
                any(), any(), any(PaymentEvent.class));
    }

    @Test
    void getPaymentByOrderId_ShouldReturnPayment_WhenExists() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(199.99))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("test-transaction-id")
                .build();

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals("COMPLETED", response.getStatus());
    }

    @Test
    void getPaymentByOrderId_ShouldThrowException_WhenNotFound() {
        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentByOrderId(1L));
    }
}