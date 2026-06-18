package com.webshop.notification;

import com.webshop.notification.dto.NotificationEvent;
import com.webshop.notification.dto.OrderItemResponse;
import com.webshop.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void sendOrderConfirmation_ShouldSendEmail_WhenValidEvent() {
        NotificationEvent event = NotificationEvent.builder()
                .orderId(1L)
                .userId(1L)
                .status("COMPLETED")
                .totalAmount(BigDecimal.valueOf(199.99))
                .shippingAddress("123 Main St")
                .items(List.of(
                        OrderItemResponse.builder()
                                .productName("Test Product")
                                .quantity(2)
                                .price(BigDecimal.valueOf(99.99))
                                .build()
                ))
                .build();

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendOrderConfirmation(event);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmation_ShouldNotThrow_WhenMailFails() {
        NotificationEvent event = NotificationEvent.builder()
                .orderId(1L)
                .userId(1L)
                .status("COMPLETED")
                .totalAmount(BigDecimal.valueOf(199.99))
                .shippingAddress("123 Main St")
                .build();

        doThrow(new RuntimeException("Mail server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendOrderConfirmation(event);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}