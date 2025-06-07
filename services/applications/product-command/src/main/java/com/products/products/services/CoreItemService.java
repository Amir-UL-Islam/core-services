package com.products.products.services;

import com.products.products.model.dtos.NewItemRequest;
import com.products.products.model.dtos.NewCreatedItemResponse;
import com.products.products.model.entitis.CoreItem;
import com.products.products.repository.CoreItemRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CoreItemService {
    private final CoreItemRepo coreItemRepo;

    @Autowired
    public CoreItemService(CoreItemRepo coreItemRepo) {
        this.coreItemRepo = coreItemRepo;
    }

    public void create(NewItemRequest newItemRequest) {
        CoreItem newItem = CoreItem.builder()
                .name(newItemRequest.getName())
                .price(newItemRequest.getPrice())
                .description(newItemRequest.getDescription())
                .build();
        CoreItem entity = coreItemRepo.save(newItem);
        log.info("New Item to be added in Record is this: {} ", entity);
    }

    public List<NewCreatedItemResponse> find() {
        return coreItemRepo.findAll().stream().map(item -> NewCreatedItemResponse.builder()
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .build()).toList();
    }
}
