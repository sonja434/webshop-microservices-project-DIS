package com.webshop.notification;

import com.webshop.notification.dto.NotificationEvent;
import com.webshop.notification.dto.OrderItemResponse;
import com.webshop.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

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