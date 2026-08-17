package com.mcp.inventory.dto;

import java.math.BigDecimal;

import com.mcp.inventory.domain.InventoryStatus;
import com.mcp.inventory.domain.ProductType;

public record InventoryItem(
        String productId,
        String sku,
        String productName,
        ProductType productType,
        String brand,
        String description,
        BigDecimal price,
        int availableQuantity,
        InventoryStatus status
) {
}
