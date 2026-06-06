package com.webshop.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}/available")
    boolean isAvailable(@PathVariable Long productId,
                        @RequestParam Integer quantity);

    @PutMapping("/api/inventory/{productId}/reduce")
    void reduceStock(@PathVariable Long productId,
                     @RequestParam Integer quantity);
}