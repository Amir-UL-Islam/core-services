package com.orders.services;

import com.orders.model.dto.OrderStackDto;
import com.orders.model.dto.PlacedItemsDto;
import com.orders.model.entites.OrderStack;
import com.orders.model.entites.PlacedItems;
import com.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;


    public void createOrder(OrderStackDto orderStackDto) {

        OrderStack orderStack = new OrderStack();
        orderStack.setOrderNumber(UUID.randomUUID().toString());
        List<PlacedItems> items = orderStackDto.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapTo)
                .toList();
        orderStack.setOrderItems(items);
        orderRepository.save(orderStack);
    }

    private PlacedItems mapTo(PlacedItemsDto placedItem) {
        PlacedItems placedItems = new PlacedItems();
        placedItems.setSkuCode(placedItem.getSkuCode());
        placedItems.setUnitePrice(placedItem.getUnitePrice());
        placedItems.setQuantity(placedItem.getQuantity());
        return placedItems;
    }
    public List<OrderStackDto> getAllOrder() {
        return orderRepository.findAll().stream().map(orderStack -> OrderStackDto.builder()
                .orderNumber(orderStack.getOrderNumber())
                .orderLineItemsDtoList(orderStack.getOrderItems().stream().map(placedItems -> PlacedItemsDto.builder()
                        .skuCode(placedItems.getSkuCode())
                        .unitePrice(placedItems.getUnitePrice())
                        .quantity(placedItems.getQuantity())
                        .build()).collect(Collectors.toList())
                ).build()).collect(Collectors.toList());
    }
}
