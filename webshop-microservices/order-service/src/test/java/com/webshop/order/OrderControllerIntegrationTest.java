package com.webshop.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webshop.order.client.InventoryClient;
import com.webshop.order.client.ProductClient;
import com.webshop.order.dto.OrderItemRequest;
import com.webshop.order.dto.OrderRequest;
import com.webshop.order.dto.ProductResponse;
import com.webshop.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private InventoryClient inventoryClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_ShouldReturn201_WhenValidRequest() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .active(true)
                .build();

        when(productClient.getProductById(1L)).thenReturn(product);
        when(inventoryClient.isAvailable(1L, 2)).thenReturn(true);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.shippingAddress").value("123 Main St"));
    }

    @Test
    void getOrderById_ShouldReturn200_WhenExists() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .active(true)
                .build();

        when(productClient.getProductById(1L)).thenReturn(product);
        when(inventoryClient.isAvailable(1L, 2)).thenReturn(true);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");
        request.setItems(List.of(item));

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getMyOrders_ShouldReturn200_WhenUserHasOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}