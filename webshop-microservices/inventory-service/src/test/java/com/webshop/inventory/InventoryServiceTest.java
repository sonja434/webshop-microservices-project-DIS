package com.webshop.inventory;

import com.webshop.inventory.dto.InventoryRequest;
import com.webshop.inventory.dto.InventoryResponse;
import com.webshop.inventory.model.Inventory;
import com.webshop.inventory.repository.InventoryRepository;
import com.webshop.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void addInventory_ShouldReturnInventoryResponse_WhenValidRequest() {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> {
                    Inventory inv = invocation.getArgument(0);
                    inv.setId(1L);
                    return inv;
                });

        InventoryResponse response = inventoryService.addInventory(request);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals(100, response.getQuantity());
    }

    @Test
    void addInventory_ShouldThrowException_WhenAlreadyExists() {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> inventoryService.addInventory(request));
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenSufficientStock() {
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(inventory));

        boolean result = inventoryService.isAvailable(1L, 50);

        assertTrue(result);
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenInsufficientStock() {
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .quantity(10)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(inventory));

        boolean result = inventoryService.isAvailable(1L, 50);

        assertFalse(result);
    }

    @Test
    void reduceStock_ShouldReduceQuantity_WhenSufficientStock() {
        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        inventoryService.reduceStock(1L, 10);

        assertEquals(90, inventory.getQuantity());
        verify(inventoryRepository, times(1)).save(inventory);
    }
}