package com.inventory.controller;

import com.inventory.model.dto.InventoryStockResponse;
import com.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("api/v1/check-is-in-stock")
    @ApiResponse(description = "Returns true if the skuCode is present in the database")
    public boolean checkInventory(
            @RequestParam("skuCode") String skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }

    @GetMapping("api/v2/check-is-in-stock")
    @ApiResponse(description = "Returns true if any of the skuCode is present in the database")
    public boolean checkInventory(
            @RequestParam("skuCode") Set<String> skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }

    @GetMapping("api/v3/check-is-in-stock")
    @ApiResponse(description = "Returns a list of InventoryStockResponse objects along with the inStock status")
    public List<InventoryStockResponse> checkInventory(
            @RequestParam("skuCode") List<String> skuCode
    ) {
        return inventoryService.isInStock(skuCode);
    }
}
