package com.webshop.inventory.service;

import com.webshop.inventory.dto.InventoryRequest;
import com.webshop.inventory.dto.InventoryResponse;
import com.webshop.inventory.model.Inventory;
import com.webshop.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryResponse addInventory(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new RuntimeException("Inventory already exists for product: "
                    + request.getProductId());
        }

        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();

        inventoryRepository.save(inventory);
        return mapToResponse(inventory);
    }

    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory not found for product: " + productId));
        return mapToResponse(inventory);
    }

    public boolean isAvailable(Long productId, Integer quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> inv.getAvailableQuantity() >= quantity)
                .orElse(false);
    }

    @Transactional
    public void reduceStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory not found for product: " + productId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: "
                    + productId);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
        log.info("Stock reduced for product: {} by: {}", productId, quantity);
    }

    @Transactional
    public InventoryResponse updateInventory(Long productId,
                                             InventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory not found for product: " + productId));

        inventory.setQuantity(request.getQuantity());
        inventoryRepository.save(inventory);
        return mapToResponse(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .build();
    }
}