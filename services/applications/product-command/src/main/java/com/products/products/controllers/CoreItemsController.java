package com.products.products.controllers;

import com.products.products.model.dtos.ItemDTO;
import com.products.products.services.CoreItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class CoreItemsController {

    private final CoreItemService itemsService;

    @Autowired
    public CoreItemsController(CoreItemService itemsService) {
        this.itemsService = itemsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody ItemDTO item) {
        itemsService.create(item);
    }

}
