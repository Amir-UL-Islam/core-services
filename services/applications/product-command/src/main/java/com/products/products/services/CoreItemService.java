package com.products.products.services;

import com.products.products.model.dtos.ItemDTO;
import com.products.products.model.entitis.CoreItem;
import com.products.products.repository.CoreItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoreItemService {
    private final CoreItemRepository coreItemRepo;

    @Autowired
    public CoreItemService(CoreItemRepository coreItemRepo) {
        this.coreItemRepo = coreItemRepo;
    }

    public void create(ItemDTO newItemRequest) {
        CoreItem newItem = CoreItem.builder()
                .name(newItemRequest.getName())
                .price(newItemRequest.getPrice())
                .description(newItemRequest.getDescription())
                .build();
        CoreItem entity = coreItemRepo.save(newItem);
        log.info("New Item to be added in Record is this: {} ", entity);
    }
}
