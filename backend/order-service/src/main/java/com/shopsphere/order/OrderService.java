package com.shopsphere.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.OrderApi.CreateOrderRequest;
import com.shopsphere.order.OrderApi.ItemRequest;
import com.shopsphere.order.OrderApi.OrderCreatedEvent;
import com.shopsphere.order.OrderApi.OrderShippedEvent;
import com.shopsphere.order.OrderApi.ProductLookup;
import com.shopsphere.order.OrderApi.ReservationItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String ORDER_CREATED = "order.created";
    private static final String ORDER_SHIPPED = "order.shipped";
    private static final String PAYMENT_COMPLETED = "payment.completed";
    private static final String PAYMENT_FAILED = "payment.failed";
    private static final String INVENTORY_INSUFFICIENT = "inventory.insufficient";

    private final OrderRepository orders;
    private final ProcessedEventRepository processedEvents;
    private final KafkaTemplate<String, Object> kafka;
    private final RestClient products;
    private final ObjectMapper mapper;

    public OrderService(OrderRepository orders,
                        ProcessedEventRepository processedEvents,
                        KafkaTemplate<String, Object> kafka,
                        RestClient.Builder productsBuilder,
                        ObjectMapper mapper) {
        this.orders = orders;
        this.processedEvents = processedEvents;
        this.kafka = kafka;
        this.mapper = mapper;
        this.products = productsBuilder.baseUrl("http://product-service").build();
    }

    /**
     * Resolves catalog prices once per line, persists the order, then publishes {@code order.created}.
     *
     * <p>The catalog snapshot and the reservation request are built from the same lookup so a price
     * or SKU change between two HTTP calls can never split the two views of the order.
     */
    @Transactional
    public OrderEntity create(Long userId, CreateOrderRequest request) {
        List<ReservationItem> reservation = new ArrayList<>();
        List<OrderItemEntity> lines = new ArrayList<>();

        for (ItemRequest item : request.items()) {
            ProductLookup product = lookup(item.productId());
            if (!product.active()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product " + item.productId() + " is not available");
            }
            lines.add(new OrderItemEntity(product.id(), product.name(), item.quantity(), product.price()));
            reservation.add(new ReservationItem(product.id(), product.sku(), item.quantity()));
        }

        BigDecimal total = lines.stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity order = new OrderEntity(userId, total);
        lines.forEach(order::addItem);
        OrderEntity saved = orders.save(order);

        kafka.send(ORDER_CREATED, String.valueOf(saved.getId()), new OrderCreatedEvent(
                UUID.randomUUID().toString(), saved.getId(), userId, total, reservation, Instant.now()));

        log.info("order created id={} userId={} lines={} total={}", saved.getId(), userId, lines.size(), total);
        return saved;
    }

    /**
     * Marks a paid order as shipped and announces it on {@code order.shipped}.
     * Returns {@code null} when the order cannot move to SHIPPED from its current state.
     */
    @Transactional
    public OrderEntity ship(Long orderId) {
        OrderEntity order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.ship()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order cannot be shipped from status " + order.getStatus());
        }
        OrderEntity saved = orders.save(order);

        kafka.send(ORDER_SHIPPED, String.valueOf(saved.getId()), new OrderShippedEvent(
                UUID.randomUUID().toString(), saved.getId(), saved.getUserId(), saved.getTotalAmount(),
                saved.getStatus().name(), Instant.now()));

        log.info("order shipped id={} userId={}", saved.getId(), saved.getUserId());
        return saved;
    }

    @KafkaListener(topics = PAYMENT_COMPLETED, groupId = "order-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @Transactional
    public void onPaymentCompleted(String payload) throws Exception {
        PaymentCompletedEvent event = mapper.readValue(payload, PaymentCompletedEvent.class);
        if (alreadyHandled(event.eventId())) {
            return;
        }
        orders.findById(event.orderId()).ifPresent(order -> {
            if (order.markPaid()) {
                log.info("order {} marked PAID", order.getId());
            }
            orders.save(order);
        });
        recordHandled(event.eventId(), PAYMENT_COMPLETED);
    }

    @KafkaListener(topics = PAYMENT_FAILED, groupId = "order-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @Transactional
    public void onPaymentFailed(String payload) throws Exception {
        PaymentFailedEvent event = mapper.readValue(payload, PaymentFailedEvent.class);
        if (alreadyHandled(event.eventId())) {
            return;
        }
        orders.findById(event.orderId()).ifPresent(order -> {
            if (order.cancel()) {
                log.info("order {} cancelled after payment failure: {}", order.getId(), event.reason());
            }
            orders.save(order);
        });
        recordHandled(event.eventId(), PAYMENT_FAILED);
    }

    @KafkaListener(topics = INVENTORY_INSUFFICIENT, groupId = "order-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @Transactional
    public void onInventoryInsufficient(String payload) throws Exception {
        InventoryFailureEvent event = mapper.readValue(payload, InventoryFailureEvent.class);
        if (alreadyHandled(event.eventId())) {
            return;
        }
        orders.findById(event.orderId()).ifPresent(order -> {
            if (order.cancel()) {
                log.info("order {} cancelled: {}", order.getId(), event.reason());
            }
            orders.save(order);
        });
        recordHandled(event.eventId(), INVENTORY_INSUFFICIENT);
    }

    private ProductLookup lookup(Long productId) {
        ProductLookup product = products.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductLookup.class);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product " + productId + " does not exist");
        }
        return product;
    }

    private boolean alreadyHandled(String eventId) {
        return processedEvents.existsByEventId(eventId);
    }

    private void recordHandled(String eventId, String topic) {
        processedEvents.save(new ProcessedEvent(eventId, topic));
    }

    public record PaymentCompletedEvent(String eventId, Long orderId, Long userId,
                                        BigDecimal amount, String status, String transactionReference,
                                        Instant occurredAt) {
    }

    public record PaymentFailedEvent(String eventId, Long orderId, Long userId,
                                     BigDecimal amount, String reason, Instant occurredAt) {
    }

    public record InventoryFailureEvent(String eventId, Long orderId, Long userId,
                                        BigDecimal amount, String reason, Instant occurredAt) {
    }
}