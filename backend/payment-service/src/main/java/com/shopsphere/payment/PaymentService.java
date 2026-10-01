package com.shopsphere.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String INVENTORY_RESERVED = "inventory.reserved";
    private static final String PAYMENT_COMPLETED = "payment.completed";
    private static final String PAYMENT_FAILED = "payment.failed";

    private final PaymentProcessor processor;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;

    public PaymentService(PaymentProcessor processor,
                          KafkaTemplate<String, Object> kafka,
                          ObjectMapper mapper) {
        this.processor = processor;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    /**
     * A reservation is the only trigger for charging: money is never taken before stock is secured.
     *
     * <p>{@link PaymentProcessor#charge} commits on its own, so the event below is published only
     * after the payment row is durable. This narrows but does not eliminate the publish/DB gap;
     * closing it fully needs a transactional outbox (see {@code docs/deployment.md}).
     */
    @KafkaListener(topics = INVENTORY_RESERVED, groupId = "payment-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onInventoryReserved(String payload) throws Exception {
        InventoryReservedEvent event = mapper.readValue(payload, InventoryReservedEvent.class);
        Payment payment = processor.charge(event.orderId(), event.userId(), event.amount());
        publishResult(payment);
    }

    private void publishResult(Payment payment) {
        boolean approved = payment.getStatus() == PaymentStatus.SUCCESS;
        String topic = approved ? PAYMENT_COMPLETED : PAYMENT_FAILED;
        log.info("publishing {} for orderId={}", topic, payment.getOrderId());
        kafka.send(topic, String.valueOf(payment.getOrderId()), new PaymentEvent(
                UUID.randomUUID().toString(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getTransactionReference(),
                approved ? null : "Payment was declined by the simulated processor",
                Instant.now()));
    }

    public record InventoryReservedEvent(String eventId, Long orderId, Long userId,
                                         BigDecimal amount, String reason, Instant occurredAt) {
    }

    public record PaymentEvent(String eventId, Long orderId, Long userId, BigDecimal amount,
                               String status, String transactionReference, String reason,
                               Instant occurredAt) {
    }
}