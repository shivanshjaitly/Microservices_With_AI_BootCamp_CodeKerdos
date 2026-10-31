package in.codekerdos.notification.dto;

import in.codekerdos.notification.entity.NotificationLog;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String orderId,
        String eventType,
        String message,
        Instant sentAt
) {
    public static NotificationResponse from(NotificationLog log) {
        return new NotificationResponse(
                log.getId(),
                log.getOrderId(),
                log.getEventType(),
                log.getMessage(),
                log.getSentAt()
        );
    }
}
