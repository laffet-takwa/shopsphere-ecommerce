package com.shopsphere.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void trimsProductTextAndDefaultsNewProductsToActive() {
        var request = new ProductRequest(" Lamp ", " Soft light ", new BigDecimal("12.50"),
                " Home ", " LAMP-1 ", null, null);
        var product = new Product(request);

        assertEquals("Lamp", product.getName());
        assertEquals("Home", product.getCategory());
        assertEquals("LAMP-1", product.getSku());
        assertTrue(product.isActive());
    }
}