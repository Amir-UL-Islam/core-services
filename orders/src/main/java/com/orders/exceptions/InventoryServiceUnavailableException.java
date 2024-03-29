package com.orders.exceptions;

public class InventoryServiceUnavailableException extends RuntimeException {
    public InventoryServiceUnavailableException(String s) {
        super(s);
    }
}
