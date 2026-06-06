package com.webshop.inventory.controller;

import com.webshop.inventory.dto.InventoryRequest;
import com.webshop.inventory.dto.InventoryResponse;
import com.webshop.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.addInventory(request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Boolean> isAvailable(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(
                inventoryService.isAvailable(productId, quantity));
    }

    @PutMapping("/{productId}/reduce")
    public ResponseEntity<Void> reduceStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        inventoryService.reduceStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(
                inventoryService.updateInventory(productId, request));
    }
}