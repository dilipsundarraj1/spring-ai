package com.mcp.inventory.mcp;

import java.util.List;

import com.mcp.inventory.domain.ProductType;
import com.mcp.inventory.dto.InventoryItem;
import com.mcp.inventory.service.InventoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * Exposes the read side of the inventory as MCP tools (SYNC, streamable HTTP).
 * Write operations (add/update/delete) stay REST-only on purpose.
 */
@Service
public class InventoryTools {

    private static final Logger log = LoggerFactory.getLogger(InventoryTools.class);

    private final InventoryService inventoryService;

    public InventoryTools(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @McpTool(description = "Get a single inventory item of the electronics store by its productId. "
            + "Returns the item's sku, name, type, brand, price, available quantity and stock status.")
    public InventoryItem getInventoryItemByProductId(
            @McpToolParam(description = "The unique productId (UUID) of the inventory item") String productId) {
        log.info("MCP tool getInventoryItemByProductId: {}", productId);
        return inventoryService.getItemByProductId(productId);
    }

    @McpTool(description = "List all inventory items of the electronics store for a given product type.")
    public List<InventoryItem> getInventoryItemsByProductType(
            @McpToolParam(description = "The product type to filter by, e.g. MOBILE, LAPTOP, TV")
            ProductType productType) {
        log.info("MCP tool getInventoryItemsByProductType: {}", productType);
        return inventoryService.getItemsByProductType(productType);
    }

    @McpTool(description = "Search inventory items of the electronics store by product name. "
            + "The match is case-insensitive and matches partial names, e.g. 'iphone' or 'watch'.")
    public List<InventoryItem> searchInventoryItemsByProductName(
            @McpToolParam(description = "Full or partial product name to search for") String productName) {
        log.info("MCP tool searchInventoryItemsByProductName: {}", productName);
        return inventoryService.searchItemsByProductName(productName);
    }

    @McpTool(description = "List every inventory item of the electronics store, "
            + "including price, available quantity and stock status.")
    public List<InventoryItem> getAllInventoryItems() {
        log.info("MCP tool getAllInventoryItems");
        return inventoryService.getAllItems();
    }
}
