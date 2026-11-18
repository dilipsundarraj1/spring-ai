package com.mcp.inventory.controller;

import java.net.URI;
import java.util.List;

import com.mcp.inventory.domain.ProductType;
import com.mcp.inventory.dto.InventoryItem;
import com.mcp.inventory.dto.InventoryItemRequest;
import com.mcp.inventory.service.InventoryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryItem> addItem(@Valid @RequestBody InventoryItemRequest request) {
        var item = inventoryService.addItem(request);
        return ResponseEntity.created(URI.create("/v1/inventory/" + item.productId())).body(item);
    }

    @PutMapping("/{productId}")
    public InventoryItem updateItem(@PathVariable String productId,
                                    @Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.updateItem(productId, request);
    }

    @GetMapping("/{productId}")
    public InventoryItem getItemByProductId(@PathVariable String productId) {
        return inventoryService.getItemByProductId(productId);
    }

    @GetMapping
    public List<InventoryItem> getItems(@RequestParam(required = false) ProductType productType,
                                        @RequestParam(required = false) String productName) {
        if (productType != null) {
            return inventoryService.getItemsByProductType(productType);
        }
        if (productName != null) {
            return inventoryService.searchItemsByProductName(productName);
        }
        return inventoryService.getAllItems();
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable String productId) {
        inventoryService.deleteItem(productId);
    }
}
