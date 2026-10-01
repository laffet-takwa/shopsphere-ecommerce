package com.shopsphere.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class InventoryItemTest {
    @Test
    void reservesOnceAndReleasesAvailableStock() {
        InventoryItem item = new InventoryItem(1001L, "HOME-LAMP-01", 5);

        assertTrue(item.reserve(71L, 3));
        assertTrue(item.reserve(71L, 3));
        assertEquals(2, item.availableQuantity());
        item.release(71L);
        assertEquals(5, item.availableQuantity());
    }

    @Test
    void refusesReservationsBeyondAvailableStock() {
        InventoryItem item = new InventoryItem(1001L, "HOME-LAMP-01", 2);

        assertFalse(item.reserve(71L, 3));
        assertEquals(2, item.availableQuantity());
    }

    @Test
    void tracksReservationsPerOrder() {
        InventoryItem item = new InventoryItem(1001L, "HOME-LAMP-01", 10);

        assertTrue(item.reserve(71L, 3));
        assertTrue(item.reserve(72L, 4));

        assertTrue(item.hasReservation(71L));
        assertTrue(item.hasReservation(72L));
        assertFalse(item.hasReservation(73L));
        assertEquals(7, item.getReservedQuantity());
        assertEquals(3, item.availableQuantity());
    }

    @Test
    void releasingAnUnheldOrderDoesNotChangeStock() {
        InventoryItem item = new InventoryItem(1001L, "HOME-LAMP-01", 10);
        item.reserve(71L, 3);

        item.release(999L);

        assertEquals(3, item.getReservedQuantity());
        assertFalse(item.hasReservation(999L));
        assertTrue(item.hasReservation(71L));
    }

    @Test
    void refusesNonPositiveReservations() {
        InventoryItem item = new InventoryItem(1001L, "HOME-LAMP-01", 5);

        assertFalse(item.reserve(71L, 0));
        assertEquals(5, item.availableQuantity());
    }
}
