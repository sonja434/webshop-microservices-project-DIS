package com.webshop.payment;

import com.webshop.payment.dto.PaymentEvent;
import com.webshop.payment.model.Payment;
import com.webshop.payment.repository.PaymentRepository;
import com.webshop.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void processPayment_ShouldCreatePayment_WhenValidEvent() {
        PaymentEvent event = PaymentEvent.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(199.99))
                .status("PENDING")
                .shippingAddress("123 Main St")
                .build();

        paymentService.processPayment(event);

        Payment payment = paymentRepository.findByOrderId(1L).orElse(null);
        assertNotNull(payment);
        assertEquals(Payment.PaymentStatus.COMPLETED, payment.getStatus());
        assertNotNull(payment.getTransactionId());
    }

    @Test
    void getPaymentByOrderId_ShouldReturn200_WhenExists() throws Exception {
        Payment payment = Payment.builder()
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(199.99))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("test-transaction-id")
                .build();

        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/order/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getPaymentsByUserId_ShouldReturn200_WhenExists() throws Exception {
        Payment payment = Payment.builder()
                .orderId(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(199.99))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("test-transaction-id")
                .build();

        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/user")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value(1));
    }
}