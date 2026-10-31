package in.codekerdos.notification.controller;

import in.codekerdos.notification.dto.NotificationResponse;
import in.codekerdos.notification.exception.ResourceNotFoundException;
import in.codekerdos.notification.repository.NotificationLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Small demo scale — no pagination needed for GET /api/notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationController(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationLogRepository.findAll().stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/{orderId}")
    public List<NotificationResponse> getByOrderId(@PathVariable String orderId) {
        List<NotificationResponse> notifications = notificationLogRepository.findByOrderId(orderId).stream()
                .map(NotificationResponse::from)
                .toList();
        if (notifications.isEmpty()) {
            throw new ResourceNotFoundException("No notifications found for order " + orderId);
        }
        return notifications;
    }
}
