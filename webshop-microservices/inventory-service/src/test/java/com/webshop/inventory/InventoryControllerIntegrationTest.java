package com.webshop.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webshop.inventory.dto.InventoryRequest;
import com.webshop.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    @Test
    void addInventory_ShouldReturn201_WhenValidRequest() throws Exception {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenSufficientStock() throws Exception {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/1/available")
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenInsufficientStock() throws Exception {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(10);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/1/available")
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void reduceStock_ShouldReturn200_WhenSufficientStock() throws Exception {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/inventory/1/reduce")
                        .param("quantity", "10"))
                .andExpect(status().isOk());
    }
}