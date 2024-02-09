package com.orders.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStackDto {
    private String orderNumber;
    private List<PlacedItemsDto> orderLineItemsDtoList;
}