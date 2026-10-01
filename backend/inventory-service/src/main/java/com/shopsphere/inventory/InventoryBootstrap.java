package com.shopsphere.inventory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryBootstrap {
    @Bean
    ApplicationRunner seedInventory(InventoryRepository inventory) {
        return args -> {
            Object[][] sampleStock = {
                {1001L, "HOME-LAMP-01", 12}, {1002L, "HOME-CHAIR-02", 4},
                {1003L, "TECH-CHARGER-03", 30}, {1004L, "TECH-AUDIO-04", 9},
                {1005L, "WELL-OIL-05", 24}, {1006L, "WELL-CANDLE-06", 18},
                {1007L, "ACC-WATCH-07", 6}, {1008L, "ACC-TOTE-08", 20}
            };
            for (Object[] row : sampleStock) {
                Long productId = (Long) row[0];
                if (inventory.findByProductId(productId).isEmpty()) {
                    inventory.save(new InventoryItem(productId, (String) row[1], (int) row[2]));
                }
            }
        };
    }
}