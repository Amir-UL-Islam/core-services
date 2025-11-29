package com.products.products.services;

import com.products.products.model.dtos.ItemDTO;
import com.products.products.repository.CoreItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreItemService {
    private final CoreItemRepository coreItemRepo;

    public List<ItemDTO> find() {
        return coreItemRepo.findAll().stream().map(item -> ItemDTO.builder()
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .build()).toList();
    }
}
