package in.codekerdos.notification.service;

import in.codekerdos.notification.entity.NotificationLog;
import in.codekerdos.notification.event.OrderEvent;
import in.codekerdos.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds a human-readable notification message for a terminal order event
 * and "sends" it — for this teaching demo, sending == logging + persisting a
 * NotificationLog row (no real email/SMS provider wired up).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public void notifyCustomer(OrderEvent event) {
        String message = buildMessage(event);

        NotificationLog notificationLog = new NotificationLog(event.orderId(), event.type(), message);
        notificationLogRepository.save(notificationLog);

        log.info("Notification for order {} ({}): {}", event.orderId(), event.type(), message);
    }

    private String buildMessage(OrderEvent event) {
        return switch (event.type()) {
            case OrderEvent.ORDER_APPROVED ->
                    "Your order #" + event.orderId() + " has been approved and is being processed.";
            case OrderEvent.ORDER_HELD ->
                    "Your order #" + event.orderId() + " is on hold pending manual review.";
            case OrderEvent.ORDER_REJECTED -> {
                String reasoning = extractReasoning(event);
                yield "Your order #" + event.orderId() + " was rejected."
                        + (reasoning != null ? " " + reasoning : "");
            }
            default -> "Update on order #" + event.orderId() + ": " + event.type();
        };
    }

    private String extractReasoning(OrderEvent event) {
        if (event.payload() == null) {
            return null;
        }
        Object reasoning = event.payload().get("reasoning");
        return reasoning != null ? String.valueOf(reasoning) : null;
    }
}
