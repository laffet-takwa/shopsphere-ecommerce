package com.shopsphere.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(@NotBlank @Size(max = 160) String name,
                             @NotBlank @Size(max = 4000) String description,
                             @NotNull @DecimalMin(value = "0.01") BigDecimal price,
                             @NotBlank @Size(max = 80) String category,
                             @NotBlank @Size(max = 64) String sku,
                             @Size(max = 1000) String imageUrl,
                             Boolean active) {
    public ProductRequest {
        active = active == null || active;
    }
}