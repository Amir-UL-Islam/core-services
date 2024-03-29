package com.inventory.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryStockResponse {
    private String skuCode;
    private boolean inStock;
}
