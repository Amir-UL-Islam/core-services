package com.orders.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacedItemsDto implements Serializable {
    private String skuCode;

    private Long unitePrice;

    private Long quantity;

}
