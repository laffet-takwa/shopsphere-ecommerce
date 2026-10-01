package com.shopsphere.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class NotificationTest {
    @Test
    void marksAStoredNotificationAsRead() {
        var notification = new Notification("event-1", 7L, "payment.completed", "Payment received");

        assertFalse(notification.isRead());
        notification.markRead();
        assertTrue(notification.isRead());
    }
}