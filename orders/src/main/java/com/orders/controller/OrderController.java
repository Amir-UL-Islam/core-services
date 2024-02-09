package com.orders.controller;

import com.orders.model.dto.OrderStackDto;
import com.orders.services.OrderService;
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
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }


    @PostMapping
    public void createOrder(@RequestBody OrderStackDto orderStackDto) {
        orderService.createOrder(orderStackDto);
        log.info("Order placed successfully");
    }

    @GetMapping
    public List<OrderStackDto> getAllOrder() {
        return orderService.getAllOrder();
    }


    //TEST Alozo
    @GetMapping("/test")
    public ResponseEntity<Objects> test() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-API-KEY", "alozo-api-user-app");
        headers.set("X-API-SECRET", "zKk3KgMJG4XJbHuHssPuCBJQ");

        HttpEntity<String> entity = new HttpEntity<>("body", headers);


        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                "http://localhost:8080/test",
                HttpMethod.GET,
                entity,
                Objects.class
        );

    }

}
