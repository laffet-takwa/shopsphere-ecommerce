package com.shopsphere.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @Column(nullable = false) private Long productId;
    @Column(nullable = false, length = 160) private String productName;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;

    protected OrderItemEntity() {}
    public OrderItemEntity(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId; this.productName = productName; this.quantity = quantity;
        this.unitPrice = unitPrice; this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    void setOrder(OrderEntity order) { this.order = order; }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}