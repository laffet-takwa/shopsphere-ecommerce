package com.shopsphere.notification;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("notifications")
public class Notification {
    @Id private String id;
    @org.springframework.data.mongodb.core.index.Indexed(unique = true) private String eventId;
    @Indexed private Long userId;
    private String type;
    private String message;
    private Instant createdAt;
    private boolean read;
    protected Notification() {}
    public Notification(String eventId, Long userId, String type, String message) {
        this.eventId = eventId; this.userId = userId; this.type = type; this.message = message; this.createdAt = Instant.now();
    }
    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void markRead() { read = true; }
}