package com.shopsphere.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderApi {
    private OrderApi() {}
    public record CreateOrderRequest(@NotEmpty List<@Valid ItemRequest> items) {}
    public record ItemRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record ItemResponse(Long productId, String productName, int quantity,
                               BigDecimal unitPrice, BigDecimal subtotal) {}
    public record OrderResponse(Long id, Long userId, OrderStatus status, BigDecimal totalAmount,
                                Instant createdAt, Instant updatedAt, List<ItemResponse> items) {
        static OrderResponse from(OrderEntity order) {
            return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(),
                    order.getCreatedAt(), order.getUpdatedAt(), order.getItems().stream()
                    .map(item -> new ItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(),
                            item.getUnitPrice(), item.getSubtotal())).toList());
        }
    }
    public record ProductLookup(Long id, String name, BigDecimal price, String sku, boolean active) {}
    public record OrderCreatedEvent(String eventId, Long orderId, Long userId, BigDecimal totalAmount,
                                    List<ReservationItem> items, Instant occurredAt) {}
    public record OrderShippedEvent(String eventId, Long orderId, Long userId, BigDecimal totalAmount,
                                    String status, Instant occurredAt) {}
    public record ReservationItem(Long productId, String sku, int quantity) {}
}