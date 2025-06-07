package com.products.products.controllers;

import com.products.products.model.dtos.NewItemRequest;
import com.products.products.model.dtos.NewCreatedItemResponse;
import com.products.products.services.CoreItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<NewCreatedItemResponse> find() {
        return itemsService.find();
    }

}
