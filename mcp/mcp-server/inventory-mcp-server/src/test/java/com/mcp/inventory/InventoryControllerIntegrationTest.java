package com.mcp.inventory;

import com.mcp.inventory.domain.InventoryStatus;
import com.mcp.inventory.domain.ProductType;
import com.mcp.inventory.dto.InventoryItem;
import com.mcp.inventory.dto.InventoryItemRequest;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // roll back each test so the data.sql seed stays intact
class InventoryControllerIntegrationTest {

    private static final String SEEDED_IPHONE_ID = "7f2c1a3e-9b4d-4c5f-8e6a-1d2b3c4d5e6f";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryItemRequest sampleRequest() {
        return new InventoryItemRequest(
                "TAB-APL-IPADPRO-13",
                "iPad Pro 13",
                ProductType.TABLET,
                "Apple",
                "iPad Pro 13-inch, M4, 256GB",
                new BigDecimal("1299.00"),
                25,
                null);
    }

    @Test
    void addItem_createsItemAndDerivesStatus() throws Exception {
        var response = mockMvc.perform(post("/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNotEmpty())
                .andExpect(jsonPath("$.sku", is("TAB-APL-IPADPRO-13")))
                .andExpect(jsonPath("$.status", is("IN_STOCK")))
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readValue(response, InventoryItem.class);
        assertThat(created.productId()).isNotBlank();
    }

    @Test
    void addItem_withDuplicateSku_returnsConflict() throws Exception {
        var duplicate = new InventoryItemRequest("MOB-APL-IP16P-256", "iPhone 16 Pro", ProductType.MOBILE,
                "Apple", null, new BigDecimal("1099.00"), 10, null);

        mockMvc.perform(post("/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void addItem_withInvalidPayload_returnsBadRequest() throws Exception {
        var invalid = new InventoryItemRequest("", "", null, "", null, new BigDecimal("-1"), -5, null);

        mockMvc.perform(post("/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getItemByProductId_returnsSeededItem() throws Exception {
        mockMvc.perform(get("/v1/inventory/{productId}", SEEDED_IPHONE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", is("iPhone 16 Pro")))
                .andExpect(jsonPath("$.brand", is("Apple")))
                .andExpect(jsonPath("$.productType", is("MOBILE")));
    }

    @Test
    void getItemByProductId_whenUnknown_returnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/inventory/{productId}", "no-such-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getItems_returnsAllSeededItems() throws Exception {
        mockMvc.perform(get("/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)));
    }

    @Test
    void getItems_filtersByProductType() throws Exception {
        mockMvc.perform(get("/v1/inventory").param("productType", "LAPTOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].productType", everyItem(is("LAPTOP"))));
    }

    @Test
    void getItems_searchesByProductNameCaseInsensitively() throws Exception {
        mockMvc.perform(get("/v1/inventory").param("productName", "watch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName", is("Apple Watch Ultra 3")));
    }

    @Test
    void updateItem_updatesFieldsAndRederivesStatus() throws Exception {
        var update = new InventoryItemRequest("MOB-APL-IP16P-256", "iPhone 16 Pro", ProductType.MOBILE,
                "Apple", "iPhone 16 Pro 256GB, Natural Titanium", new BigDecimal("999.00"), 3, null);

        mockMvc.perform(put("/v1/inventory/{productId}", SEEDED_IPHONE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(999.00)))
                .andExpect(jsonPath("$.availableQuantity", is(3)))
                .andExpect(jsonPath("$.status", is(InventoryStatus.LOW_STOCK.name())));
    }

    @Test
    void updateItem_whenUnknown_returnsNotFound() throws Exception {
        mockMvc.perform(put("/v1/inventory/{productId}", "no-such-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_removesItem() throws Exception {
        mockMvc.perform(delete("/v1/inventory/{productId}", SEEDED_IPHONE_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/inventory/{productId}", SEEDED_IPHONE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_whenUnknown_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/v1/inventory/{productId}", "no-such-id"))
                .andExpect(status().isNotFound());
    }
}
