package com.shopsphere.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {
    Optional<InventoryItem> findByProductId(Long productId);

    /**
     * Items that currently hold at least one reservation. Compensation filters these by order ID
     * in memory because Mongo cannot match a dynamic map key through a derived query, and this
     * path only runs on the rare payment-failure branch rather than on every stock read.
     */
    List<InventoryItem> findByReservationsIsNotEmpty();
}