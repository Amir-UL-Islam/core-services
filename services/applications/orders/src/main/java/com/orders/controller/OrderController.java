package com.orders.controller;

import com.orders.model.dto.OrderStackDto;
import com.orders.services.OrderService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;


@RestController
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }


    @PostMapping("api/v1/create-orders")
    @ApiResponse(responseCode = "200", description = "Order placed successfully")
    public void createOrder(@RequestBody OrderStackDto orderStackDto) throws IllegalAccessException {
        orderService.createOrder(orderStackDto);
        log.info("Order placed successfully");
    }

    @PostMapping("api/v2/create-orders")
    @ApiResponse(responseCode = "200", description = "Order placed successfully")
    public void createOrderII(@RequestBody OrderStackDto orderStackDto) {
        orderService.createOrderII(orderStackDto);
        log.info("Request Accepted for placing an Order Request");
    }

    @GetMapping("api/v1/get-all-orders")
    @ApiResponse(responseCode = "200", description = "Get all orders")
    public List<OrderStackDto> getAllOrder() {
        return orderService.getAllOrder();
    }
}
