package com.orders.exceptions;

public class ProductNotInStockException extends RuntimeException {
    // Custom exception class for product not in stock
    public ProductNotInStockException(String message) {
        super(message);
    }
}
