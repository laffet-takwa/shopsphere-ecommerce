package com.shopsphere.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Durable record of the Kafka events this service has already applied.
 *
 * <p>The unique constraint on {@code eventId} is what makes consumers idempotent: the row insert
 * happens in the same transaction as the aggregate update, so a redelivered record either loses the
 * race and is ignored, or triggers a constraint violation that the caller treats as "already done".
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "topic", nullable = false, length = 64)
    private String topic;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
    }

    @PrePersist
    void onCreate() {
        processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTopic() {
        return topic;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}