package com.webshop.order.service;

import com.webshop.order.client.InventoryClient;
import com.webshop.order.client.ProductClient;
import com.webshop.order.config.RabbitMQConfig;
import com.webshop.order.dto.*;
import com.webshop.order.model.Order;
import com.webshop.order.model.OrderItem;
import com.webshop.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    @CircuitBreaker(name = "product-service", fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        List<OrderItem> items = request.getItems().stream().map(itemRequest -> {
            // Sinhroni poziv ka product-service
            ProductResponse product = productClient.getProductById(
                    itemRequest.getProductId());

            // Sinhroni poziv ka inventory-service
            boolean available = inventoryClient.isAvailable(
                    itemRequest.getProductId(), itemRequest.getQuantity());

            if (!available) {
                throw new RuntimeException("Product not available: "
                        + product.getName());
            }

            // Smanjujemo zalihe
            inventoryClient.reduceStock(
                    itemRequest.getProductId(), itemRequest.getQuantity());

            return OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(total)
                .status(Order.OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        order = orderRepository.save(order);

        // Asinhroni poziv ka payment-service i notification-service
        OrderEvent event = buildOrderEvent(order);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event);

        log.info("Order created with id: {}", order.getId());
        return mapToResponse(order);
    }

    public OrderResponse createOrderFallback(OrderRequest request,
                                             Long userId, Exception ex) {
        log.error("Fallback triggered for createOrder: {}", ex.getMessage());
        throw new RuntimeException(
                "Service temporarily unavailable. Please try again later.");
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(Order.OrderStatus.valueOf(status));
        orderRepository.save(order);
        return mapToResponse(order);
    }

    private OrderEvent buildOrderEvent(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .build();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}