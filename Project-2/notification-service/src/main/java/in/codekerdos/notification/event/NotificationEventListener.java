package in.codekerdos.notification.event;

import in.codekerdos.notification.service.IdempotencyService;
import in.codekerdos.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts ONLY to the terminal order events on "order-events" — ORDER_APPROVED,
 * ORDER_HELD, ORDER_REJECTED. Every other event type on the topic (ORDER_CREATED,
 * INVENTORY_*, PAYMENT_*, RISK_SCORED) is intentionally ignored here; those are
 * intermediate saga signals that other services react to, not customer-facing
 * outcomes.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    public NotificationEventListener(IdempotencyService idempotencyService,
                                      NotificationService notificationService) {
        this.idempotencyService = idempotencyService;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderEvent(OrderEvent event) {
        if (!idempotencyService.markProcessedIfNew(event.eventId())) {
            log.info("Duplicate delivery of {} (eventId={}) — skipping", event.type(), event.eventId());
            return;
        }

        switch (event.type()) {
            case OrderEvent.ORDER_APPROVED, OrderEvent.ORDER_HELD, OrderEvent.ORDER_REJECTED ->
                    notificationService.notifyCustomer(event);
            default ->
                    log.debug("Ignoring non-terminal event {} (eventId={}) for order {}",
                            event.type(), event.eventId(), event.orderId());
        }
    }
}
