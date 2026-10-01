package com.shopsphere.inventory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory")
public class InventoryItem {
    @Id private String id;
    @Indexed(unique = true) private Long productId;
    private String sku;
    private int quantity;
    private int reservedQuantity;
    private Instant updatedAt;
    private Map<Long, Integer> reservations = new HashMap<>();

    protected InventoryItem() {}
    public InventoryItem(Long productId, String sku, int quantity) {
        this.productId = productId; this.sku = sku; this.quantity = quantity; this.updatedAt = Instant.now();
    }
    public int availableQuantity() { return quantity - reservedQuantity; }
    public boolean reserve(Long orderId, int amount) {
        if (reservations.containsKey(orderId)) return true;
        if (amount < 1 || availableQuantity() < amount) return false;
        reservations.put(orderId, amount); reservedQuantity += amount; updatedAt = Instant.now(); return true;
    }
    public void release(Long orderId) {
        Integer amount = reservations.remove(orderId);
        if (amount != null) { reservedQuantity -= amount; updatedAt = Instant.now(); }
    }
    public boolean hasReservation(Long orderId) { return reservations.containsKey(orderId); }
    public void setQuantity(int quantity) { this.quantity = quantity; updatedAt = Instant.now(); }
    public String getId() { return id; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}