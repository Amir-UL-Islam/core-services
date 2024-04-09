package com.orders.model.mappers;

import com.orders.model.dto.PlacedItemsDto;
import com.orders.model.entites.PlacedItems;
import org.springframework.stereotype.Component;

@Component
public class PlacedItemsMapper {
    public PlacedItems mapTo(PlacedItemsDto placedItem) {
        PlacedItems placedItems = new PlacedItems();
        placedItems.setSkuCode(placedItem.getSkuCode());
        placedItems.setUnitePrice(placedItem.getUnitePrice());
        placedItems.setQuantity(placedItem.getQuantity());
        return placedItems;
    }
}
