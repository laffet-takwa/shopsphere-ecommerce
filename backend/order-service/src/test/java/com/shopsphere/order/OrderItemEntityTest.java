package com.shopsphere.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemEntityTest {
    @Test
    void snapshotsTheLineSubtotal() {
        var item = new OrderItemEntity(1001L, "Lamp", 3, new BigDecimal("12.50"));

        assertEquals(new BigDecimal("37.50"), item.getSubtotal());
        assertEquals(3, item.getQuantity());
    }
}