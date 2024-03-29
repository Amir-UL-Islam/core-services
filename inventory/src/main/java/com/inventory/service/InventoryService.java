package com.inventory.service;

import com.inventory.model.dto.InventoryStockResponse;
import com.inventory.repository.DomainSpecificRepository;
import com.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DomainSpecificRepository domainSpecificRepository;

    @Transactional(readOnly = true)
    public boolean isInStock(String skuCode) {
        return inventoryRepository.existsBySkuCode(skuCode);
    }

    @Transactional(readOnly = true)
    public boolean isInStock(Set<String> skuCode) {
        return domainSpecificRepository.existsBySkuCodeIn(skuCode);
    }

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> isInStock(List<String> skuCode) {
        return inventoryRepository.findAllBySkuCodeIn(skuCode).stream().map(
                        inventory ->
                                InventoryStockResponse.builder()
                                        .skuCode(inventory.getSkuCode())
                                        .inStock(inventory.getQuantity() > 0)
                                        .build()
                )
                .toList();
    }
}
