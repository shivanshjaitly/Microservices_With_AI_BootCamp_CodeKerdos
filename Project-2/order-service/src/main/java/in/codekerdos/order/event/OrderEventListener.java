package in.codekerdos.order.event;

import in.codekerdos.order.service.IdempotencyService;
import in.codekerdos.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes every event on "order-events" in its own consumer group
 * (order-service) — Kafka fans out one copy per group, so this listener also
 * receives the events order-service itself published (ORDER_CREATED and the
 * terminal ORDER_* types), which are deliberately ignored below.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderEventListener(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void onOrderEvent(OrderEvent event) {
        if (!idempotencyService.markProcessedIfNew(event.eventId())) {
            log.info("Duplicate delivery of {} (eventId={}) — skipping", event.type(), event.eventId());
            return;
        }

        switch (event.type()) {
            case OrderEvent.INVENTORY_RESERVED -> orderService.onInventoryReserved(event.orderId());
            case OrderEvent.INVENTORY_REJECTED -> orderService.onInventoryRejected(event.orderId());
            case OrderEvent.PAYMENT_COMPLETED -> orderService.onPaymentCompleted(event.orderId());
            case OrderEvent.PAYMENT_FAILED -> orderService.onPaymentFailed(event.orderId());
            case OrderEvent.RISK_SCORED -> handleRiskScored(event);
            default -> log.debug("Ignoring {} (own outbound event or not saga-relevant)", event.type());
        }
    }

    private void handleRiskScored(OrderEvent event) {
        Map<String, Object> payload = event.payload();
        String decision = String.valueOf(payload.get("decision"));
        Integer riskScore = payload.get("riskScore") instanceof Number number ? number.intValue() : null;
        String reasoning = String.valueOf(payload.get("reasoning"));
        orderService.onRiskScored(event.orderId(), decision, riskScore, reasoning);
    }
}
