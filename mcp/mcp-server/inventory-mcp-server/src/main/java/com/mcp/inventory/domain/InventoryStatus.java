package com.mcp.inventory.domain;

public enum InventoryStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    DISCONTINUED;

    /** Threshold below which an in-stock item is reported as LOW_STOCK. */
    public static final int LOW_STOCK_THRESHOLD = 10;

    public static InventoryStatus fromQuantity(int availableQuantity) {
        if (availableQuantity <= 0) {
            return OUT_OF_STOCK;
        }
        return availableQuantity < LOW_STOCK_THRESHOLD ? LOW_STOCK : IN_STOCK;
    }
}
