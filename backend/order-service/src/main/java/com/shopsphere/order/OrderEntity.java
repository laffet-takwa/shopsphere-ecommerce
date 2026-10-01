package com.shopsphere.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OrderStatus status = OrderStatus.PENDING;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {}
    public OrderEntity(Long userId, BigDecimal totalAmount) { this.userId = userId; this.totalAmount = totalAmount; }
    public void addItem(OrderItemEntity item) { items.add(item); item.setOrder(this); }

    /**
     * Applies a status transition only when the current state allows it.
     * Returns {@code true} when the aggregate actually changed, which lets Kafka listeners
     * stay idempotent: a redelivered message simply reports "no change" and is dropped.
     */
    private boolean transitionTo(OrderStatus target, Set<OrderStatus> allowedFrom) {
        if (!allowedFrom.contains(status)) {
            return false;
        }
        status = target;
        return true;
    }

    public boolean markPaid() {
        return transitionTo(OrderStatus.PAID,
                EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED));
    }

    public boolean ship() {
        return transitionTo(OrderStatus.SHIPPED,
                EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING));
    }

    public boolean cancel() {
        return transitionTo(OrderStatus.CANCELLED,
                EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PAID, OrderStatus.PROCESSING));
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItemEntity> getItems() { return items; }
}