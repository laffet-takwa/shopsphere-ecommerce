package com.shopsphere.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              String category, String sku, String imageUrl, boolean active,
                              Instant createdAt, Instant updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getCategory(), product.getSku(), product.getImageUrl(),
                product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
    }
}