package com.webshop.order;

import com.webshop.order.client.InventoryClient;
import com.webshop.order.client.ProductClient;
import com.webshop.order.dto.*;
import com.webshop.order.model.Order;
import com.webshop.order.repository.OrderRepository;
import com.webshop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldReturnOrderResponse_WhenValidRequest() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");
        request.setItems(List.of(itemRequest));

        ProductResponse product = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .active(true)
                .build();

        when(productClient.getProductById(1L)).thenReturn(product);
        when(inventoryClient.isAvailable(1L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setItems(List.of());
            return order;
        });

        OrderResponse response = orderService.createOrder(request, 1L);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(
                any(), any(), any(OrderEvent.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenProductNotAvailable() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");
        request.setItems(List.of(itemRequest));

        ProductResponse product = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .build();

        when(productClient.getProductById(1L)).thenReturn(product);
        when(inventoryClient.isAvailable(1L, 2)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(request, 1L));
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        Order order = Order.builder()
                .id(1L)
                .userId(1L)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(199.98))
                .shippingAddress("123 Main St")
                .items(List.of())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PENDING", response.getStatus());
    }
}