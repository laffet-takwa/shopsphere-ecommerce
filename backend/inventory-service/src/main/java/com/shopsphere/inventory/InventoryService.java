package com.shopsphere.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.InventoryEvents.InventoryResponse;
import com.shopsphere.inventory.InventoryEvents.InventoryResultEvent;
import com.shopsphere.inventory.InventoryEvents.Item;
import com.shopsphere.inventory.InventoryEvents.OrderCreatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private static final String ORDER_CREATED = "order.created";
    private static final String PAYMENT_FAILED = "payment.failed";
    private static final String INVENTORY_RESERVED = "inventory.reserved";
    private static final String INVENTORY_INSUFFICIENT = "inventory.insufficient";

    private final InventoryRepository inventory;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;

    public InventoryService(InventoryRepository inventory,
                            KafkaTemplate<String, Object> kafka,
                            ObjectMapper mapper) {
        this.inventory = inventory;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    /**
     * Reserves every requested line or none of them: a partial reservation is rolled back before
     * {@code inventory.insufficient} is published, so stock is never held for an unfulfillable order.
     *
     * <p>Reserving the same order twice is a no-op inside {@link InventoryItem#reserve}, which makes
     * a redelivered {@code order.created} safe.
     */
    @KafkaListener(topics = ORDER_CREATED, groupId = "inventory-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onOrderCreated(String payload) throws Exception {
        OrderCreatedEvent event = mapper.readValue(payload, OrderCreatedEvent.class);

        if (!reserveAll(event)) {
            kafka.send(INVENTORY_INSUFFICIENT, String.valueOf(event.orderId()), new InventoryResultEvent(
                    UUID.randomUUID().toString(), event.orderId(), event.userId(), event.totalAmount(),
                    "Insufficient stock for one or more items", Instant.now()));
            return;
        }

        kafka.send(INVENTORY_RESERVED, String.valueOf(event.orderId()), new InventoryResultEvent(
                UUID.randomUUID().toString(), event.orderId(), event.userId(), event.totalAmount(),
                null, Instant.now()));
    }

    /**
     * Compensating action for a declined payment: whatever is still held for the order goes back
     * to available stock, otherwise a failed order would leak its reservation permanently.
     */
    @KafkaListener(topics = PAYMENT_FAILED, groupId = "inventory-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onPaymentFailed(String payload) throws Exception {
        PaymentFailedEvent event = mapper.readValue(payload, PaymentFailedEvent.class);
        releaseOrder(event.orderId());
    }

    private boolean reserveAll(OrderCreatedEvent event) {
        for (Item line : event.items()) {
            InventoryItem item = inventory.findByProductId(line.productId()).orElse(null);
            if (item == null || !item.reserve(event.orderId(), line.quantity())) {
                log.info("reservation failed orderId={} productId={}", event.orderId(), line.productId());
                releaseOrder(event.orderId());
                return false;
            }
            inventory.save(item);
        }
        log.info("stock reserved orderId={} lines={}", event.orderId(), event.items().size());
        return true;
    }

    private void releaseOrder(Long orderId) {
        List<InventoryItem> held = inventory.findByReservationsIsNotEmpty().stream()
                .filter(item -> item.hasReservation(orderId))
                .toList();
        held.forEach(item -> {
            item.release(orderId);
            inventory.save(item);
        });
        if (!held.isEmpty()) {
            log.info("released {} reservation(s) for orderId={}", held.size(), orderId);
        }
    }

    public InventoryItem get(Long productId) {
        return inventory.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public InventoryItem update(Long productId, String sku, int quantity) {
        if (quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be negative");
        }
        InventoryItem item = inventory.findByProductId(productId)
                .orElseGet(() -> new InventoryItem(productId, sku, quantity));
        if (quantity < item.getReservedQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quantity is below reserved stock");
        }
        item.setQuantity(quantity);
        return inventory.save(item);
    }

    public InventoryItem reserve(Long productId, Long orderId, int quantity) {
        InventoryItem item = get(productId);
        if (!item.reserve(orderId, quantity)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }
        return inventory.save(item);
    }

    public void release(Long productId, Long orderId) {
        InventoryItem item = get(productId);
        item.release(orderId);
        inventory.save(item);
    }

    public static InventoryResponse response(InventoryItem item) {
        return new InventoryResponse(item.getProductId(), item.getSku(), item.getQuantity(),
                item.getReservedQuantity(), item.availableQuantity(), item.getUpdatedAt());
    }

    public record PaymentFailedEvent(String eventId, Long orderId, Long userId,
                                     BigDecimal amount, String reason, Instant occurredAt) {
    }
}