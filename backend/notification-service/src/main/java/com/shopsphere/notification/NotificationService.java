package com.shopsphere.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final ObjectMapper mapper;
    public NotificationService(NotificationRepository notifications, ObjectMapper mapper) {
        this.notifications = notifications; this.mapper = mapper;
    }

    @KafkaListener(topics = {"order.created", "inventory.reserved", "inventory.insufficient", "payment.completed", "payment.failed", "order.shipped"}, groupId = "notification-service")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    public void onEvent(String payload, org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        Event event = mapper.readValue(payload, Event.class);
        if (notifications.existsByEventId(event.eventId())) return;
        String message = switch (topic) {
            case "order.created" -> "Order " + event.orderId() + " was received.";
            case "inventory.reserved" -> "Items for order " + event.orderId() + " are reserved.";
            case "inventory.insufficient" -> "Order " + event.orderId() + " could not be fulfilled: " + event.reason();
            case "payment.completed" -> "Payment for order " + event.orderId() + " completed.";
            case "payment.failed" -> "Payment for order " + event.orderId() + " failed: " + event.reason();
            case "order.shipped" -> "Order " + event.orderId() + " has shipped.";
            default -> "Order " + event.orderId() + " was updated (" + topic + ").";
        };
        notifications.save(new Notification(event.eventId(), event.userId(), topic, message));
    }

    public List<NotificationResponse> list(Long userId) {
        return notifications.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }
    public NotificationResponse markRead(String id, Long userId) {
        Notification value = notifications.findById(id).filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.markRead();
        return NotificationResponse.from(notifications.save(value));
    }
    public record NotificationResponse(String id, Long userId, String type, String message,
                                       java.time.Instant createdAt, boolean read) {
        static NotificationResponse from(Notification value) {
            return new NotificationResponse(value.getId(), value.getUserId(), value.getType(), value.getMessage(),
                    value.getCreatedAt(), value.isRead());
        }
    }
    public record Event(String eventId, Long orderId, Long userId, java.math.BigDecimal totalAmount,
                        java.math.BigDecimal amount, String reason, String status, String transactionReference,
                        java.time.Instant occurredAt) {}
}