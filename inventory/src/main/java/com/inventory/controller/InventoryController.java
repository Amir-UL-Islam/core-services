package com.inventory.controller;

import com.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
public class InventoryController {
    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("api/v1/check-is-in-stock")
    public boolean checkInventory(
            @RequestParam("skuCode") String skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }

    @GetMapping("api/v2/check-is-in-stock")
    public boolean checkInventory(
            @RequestParam("skuCode") Set<String> skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }
}
