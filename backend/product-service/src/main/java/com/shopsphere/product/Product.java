package com.shopsphere.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 4000) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 80) private String category;
    @Column(nullable = false, unique = true, length = 64) private String sku;
    @Column(length = 1000) private String imageUrl;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Product() {}

    public Product(ProductRequest request) { update(request); }
    public void update(ProductRequest request) {
        name = request.name().trim(); description = request.description().trim();
        price = request.price(); category = request.category().trim(); sku = request.sku().trim();
        imageUrl = request.imageUrl(); active = Boolean.TRUE.equals(request.active());
    }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public String getSku() { return sku; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}