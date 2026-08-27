package com.mcp.inventory.service;

import java.util.List;
import java.util.UUID;

import com.mcp.inventory.domain.ProductType;
import com.mcp.inventory.dto.InventoryItem;
import com.mcp.inventory.dto.InventoryItemRequest;
import com.mcp.inventory.entity.InventoryItemEntity;
import com.mcp.inventory.exception.InventoryItemNotFoundException;
import com.mcp.inventory.exception.SkuAlreadyExistsException;
import com.mcp.inventory.repository.InventoryItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository repository;

    public InventoryService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    public InventoryItem addItem(InventoryItemRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }
        var entity = new InventoryItemEntity(
                UUID.randomUUID().toString(),
                request.sku(),
                request.productName(),
                request.productType(),
                request.brand(),
                request.description(),
                request.price(),
                request.availableQuantity(),
                request.resolvedStatus());
        return toDto(repository.save(entity));
    }

    public InventoryItem updateItem(String productId, InventoryItemRequest request) {
        var entity = repository.findById(productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));
        if (!entity.getSku().equals(request.sku()) && repository.existsBySku(request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }
        entity.setSku(request.sku());
        entity.setProductName(request.productName());
        entity.setProductType(request.productType());
        entity.setBrand(request.brand());
        entity.setDescription(request.description());
        entity.setPrice(request.price());
        entity.setAvailableQuantity(request.availableQuantity());
        entity.setStatus(request.resolvedStatus());
        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public InventoryItem getItemByProductId(String productId) {
        return repository.findById(productId)
                .map(this::toDto)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsByProductType(ProductType productType) {
        return repository.findByProductType(productType).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> searchItemsByProductName(String productName) {
        return repository.findByProductNameContainingIgnoreCase(productName).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public void deleteItem(String productId) {
        if (!repository.existsById(productId)) {
            throw new InventoryItemNotFoundException(productId);
        }
        repository.deleteById(productId);
    }

    private InventoryItem toDto(InventoryItemEntity entity) {
        return new InventoryItem(
                entity.getProductId(),
                entity.getSku(),
                entity.getProductName(),
                entity.getProductType(),
                entity.getBrand(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getAvailableQuantity(),
                entity.getStatus());
    }
}
