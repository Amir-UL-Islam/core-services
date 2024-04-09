package com.orders.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockResponse {
    @JsonProperty("skuCode")
    private String skuCode;
    @JsonProperty("inStock")
    private boolean inStock;
}
