package com.products.products.controllers;

import com.products.products.model.dtos.NewItemRequest;
import com.products.products.model.dtos.NewCreatedItemResponse;
import com.products.products.services.CoreItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/item")
public class CoreItemsController {

    private final CoreItemService itemsService;

    @Autowired
    public CoreItemsController(CoreItemService itemsService) {
        this.itemsService = itemsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody NewItemRequest item) {
        itemsService.create(item);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<NewCreatedItemResponse> find() {
        return itemsService.find();
    }

}
