package com.mcp.inventory.exception;

public class SkuAlreadyExistsException extends RuntimeException {

    public SkuAlreadyExistsException(String sku) {
        super("An inventory item already exists with sku: " + sku);
    }
}
