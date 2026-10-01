package com.shopsphere.notification;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping
    public List<NotificationService.NotificationResponse> list(@RequestHeader("X-User-Id") Long userId) { return service.list(userId); }
    @PutMapping("/{id}/read")
    public NotificationService.NotificationResponse markRead(@PathVariable String id, @RequestHeader("X-User-Id") Long userId) {
        return service.markRead(id, userId);
    }
}