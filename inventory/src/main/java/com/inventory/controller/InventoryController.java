package com.inventory.controller;

import com.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService inventoryService;


    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public boolean checkInventory(
            @RequestParam("skuCode") String skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }

}
