package com.orders.services;

import com.orders.config.WebClintConfig;
import com.orders.exceptions.InventoryServiceUnavailableException;
import com.orders.exceptions.ProductNotInStockException;
import com.orders.model.dto.OrderStackDto;
import com.orders.model.dto.PlacedItemsDto;
import com.orders.model.dto.response.InventoryStockResponse;
import com.orders.model.entites.OrderStack;
import com.orders.model.entites.PlacedItems;
import com.orders.model.mappers.PlacedItemsMapper;
import com.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final PlacedItemsMapper mapper;
    private final WebClintConfig clintConfig;


    public void createOrder(OrderStackDto orderStackDto) throws IllegalAccessException {

        OrderStack orderStack = new OrderStack();
        orderStack.setOrderNumber(UUID.randomUUID().toString());
        List<PlacedItems> items = orderStackDto.getOrderLineItems()
                .stream()
                .map(mapper::mapTo)
                .toList();
        items.forEach(item -> item.setItemStack(orderStack));
        orderStack.setOrderItems(items);
        // Before Saving Order we need confirm weather this item is in our Inventory or not

        InventoryStockResponse[] inventoryStockResponses = clintConfig.WebClientBuilder().build()
                .get()
                .uri(
                        "http://inventory-service/api/v3/check-is-in-stock",
                        uriBuilder -> uriBuilder
                                .queryParam(
                                        "skuCode",
                                        items.stream()
                                                .map(PlacedItems::getSkuCode)
                                                .toList()
                                )
                                .build()
                )
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(InventoryStockResponse[].class)
                .block();

        if (inventoryStockResponses != null && inventoryStockResponses.length > 0) {
            boolean productInStock = Arrays.stream(inventoryStockResponses).allMatch(InventoryStockResponse::isInStock);
            if (!productInStock) {
                throw new IllegalAccessException("Product is not in stock");
            } else {
                repository.save(orderStack);
            }
        } else {
            throw new IllegalAccessException("Product is not in stock");
        }
    }

    public void createOrderII(OrderStackDto orderStackDto) {
        OrderStack orderStack = new OrderStack();
        orderStack.setOrderNumber(UUID.randomUUID().toString());
        List<PlacedItems> items = orderStackDto.getOrderLineItems()
                .stream()
                .map(mapper::mapTo)
                .toList();
        items.forEach(item -> item.setItemStack(orderStack));
        orderStack.setOrderItems(items);

        // Extract SKU codes from items
        List<String> skuCodes = items.stream()
                .map(PlacedItems::getSkuCode)
                .toList();

        // Check inventory stock asynchronously for each SKU code
        Mono<Boolean> stockCheckFlux = Flux.fromIterable(skuCodes)
                .flatMap(this::checkInventoryStock)
                .collectList()
                .map(inStockList -> inStockList.stream().allMatch(Boolean::booleanValue));

        stockCheckFlux.subscribe(
                productInStock -> {
                    if (productInStock) {
                        repository.save(orderStack);
                    } else {
                        throw new ProductNotInStockException("One or more products are not in stock");
                    }
                },
                throwable -> {
                    handleStockCheckError(throwable);
                }
        );
    }

    private Mono<Boolean> checkInventoryStock(String skuCode) {
        return clintConfig.WebClientBuilder().build()
                .get()
                .uri("http://localhost:8089/api/v3/check-is-in-stock?skuCode={skuCode}", skuCode)
                .retrieve()
                .bodyToMono(InventoryStockResponse[].class)
                .map(inventoryStockResponses -> Arrays.stream(inventoryStockResponses)
                        .allMatch(InventoryStockResponse::isInStock))
                .onErrorMap(this::handleInventoryStockError);
    }

    private void handleStockCheckError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) throwable;
            if (responseException.getStatusCode().is4xxClientError()) {
                throw new ProductNotInStockException("Failed to check inventory stock: " + responseException.getRawStatusCode());
            } else if (responseException.getStatusCode().is5xxServerError()) {
                throw new InventoryServiceUnavailableException("Inventory service is unavailable: " + responseException.getRawStatusCode());
            }
        }
        throw new RuntimeException("Failed to create order", throwable);
    }

    private Throwable handleInventoryStockError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) throwable;
            if (responseException.getStatusCode().is4xxClientError()) {
                return new ProductNotInStockException("Failed to check inventory stock: " + responseException.getRawStatusCode());
            } else if (responseException.getStatusCode().is5xxServerError()) {
                return new InventoryServiceUnavailableException("Inventory service is unavailable: " + responseException.getRawStatusCode());
            }
        }
        return new RuntimeException("Failed to check inventory stock", throwable);
    }


    public List<OrderStackDto> getAllOrder() {
        return repository.findAll().stream().map(orderStack -> OrderStackDto.builder()
                .orderNumber(orderStack.getOrderNumber())
                .orderLineItems(orderStack.getOrderItems().stream().map(placedItems -> PlacedItemsDto.builder()
                        .skuCode(placedItems.getSkuCode())
                        .unitePrice(placedItems.getUnitePrice())
                        .quantity(placedItems.getQuantity())
                        .build()).collect(Collectors.toList())
                ).build()).collect(Collectors.toList());
    }
}
