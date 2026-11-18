package com.mcp.inventory.dto;

import java.math.BigDecimal;

import com.mcp.inventory.domain.InventoryStatus;
import com.mcp.inventory.domain.ProductType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Payload for creating/updating an inventory item. The productId is assigned by
 * the server and the status, when omitted, is derived from availableQuantity.
 */
public record InventoryItemRequest(
        @NotBlank String sku,
        @NotBlank String productName,
        @NotNull ProductType productType,
        @NotBlank String brand,
        String description,
        @NotNull @Positive BigDecimal price,
        @PositiveOrZero int availableQuantity,
        InventoryStatus status
) {

    public InventoryStatus resolvedStatus() {
        return status != null ? status : InventoryStatus.fromQuantity(availableQuantity);
    }
}
