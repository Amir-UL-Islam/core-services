package com.products.products.controllers;

import com.products.products.model.dtos.ItemDTO;
import com.products.products.services.CoreItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CoreItemsController {

    private final CoreItemService itemsService;
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ItemDTO> find() {
        return itemsService.find();
    }

}
