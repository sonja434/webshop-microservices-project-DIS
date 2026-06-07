package com.webshop.notification.service;

import com.webshop.notification.config.RabbitMQConfig;
import com.webshop.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void sendOrderConfirmation(NotificationEvent event) {
        log.info("Received payment event for orderId: {}", event.getOrderId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("user" + event.getUserId() + "@example.com");
            message.setSubject("Order Confirmation - Order #" + event.getOrderId());
            message.setText(buildEmailContent(event));

            mailSender.send(message);
            log.info("Email sent for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to send email for orderId: {}",
                    event.getOrderId(), e);
        }
    }

    private String buildEmailContent(NotificationEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thank you for your order!\n\n");
        sb.append("Order ID: ").append(event.getOrderId()).append("\n");
        sb.append("Status: ").append(event.getStatus()).append("\n");
        sb.append("Shipping Address: ").append(event.getShippingAddress()).append("\n\n");
        sb.append("Items:\n");

        if (event.getItems() != null) {
            event.getItems().forEach(item -> {
                sb.append("- ").append(item.getProductName())
                        .append(" x").append(item.getQuantity())
                        .append(" = $").append(item.getPrice()).append("\n");
            });
        }

        sb.append("\nTotal Amount: $").append(event.getTotalAmount()).append("\n\n");
        sb.append("Thank you for shopping with us!");
        return sb.toString();
    }
}