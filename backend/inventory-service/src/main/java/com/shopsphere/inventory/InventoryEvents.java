package com.shopsphere.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryEvents {
    private InventoryEvents() {}
    public record OrderCreatedEvent(String eventId, Long orderId, Long userId, BigDecimal totalAmount,
                                    List<Item> items, Instant occurredAt) {}
    public record Item(Long productId, String sku, int quantity) {}
    public record InventoryResultEvent(String eventId, Long orderId, Long userId, BigDecimal amount,
                                       String reason, Instant occurredAt) {}
    public record InventoryResponse(Long productId, String sku, int quantity, int reservedQuantity,
                                    int availableQuantity, Instant updatedAt) {}
}