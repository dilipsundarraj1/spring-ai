package com.mcp.inventory.repository;

import java.util.List;

import com.mcp.inventory.domain.ProductType;
import com.mcp.inventory.entity.InventoryItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, String> {

    List<InventoryItemEntity> findByProductType(ProductType productType);

    List<InventoryItemEntity> findByProductNameContainingIgnoreCase(String productName);

    boolean existsBySku(String sku);
}
