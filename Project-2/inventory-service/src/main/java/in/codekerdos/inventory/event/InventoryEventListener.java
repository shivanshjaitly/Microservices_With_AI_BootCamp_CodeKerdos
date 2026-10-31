package in.codekerdos.inventory.event;

import in.codekerdos.inventory.service.IdempotencyService;
import in.codekerdos.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Reacts to order-events. This service's own consumer group ("inventory-service")
 * receives every event on the topic, so it must ignore anything that isn't
 * ORDER_CREATED (it will also see its own INVENTORY_RESERVED/INVENTORY_REJECTED
 * echoes, plus every other service's events).
 */
@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final IdempotencyService idempotencyService;
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public InventoryEventListener(IdempotencyService idempotencyService,
                                   InventoryService inventoryService,
                                   KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.idempotencyService = idempotencyService;
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void onOrderEvent(OrderEvent event) {
        if (!OrderEvent.ORDER_CREATED.equals(event.type())) {
            return; // not our concern — every other event type is ignored
        }

        if (!idempotencyService.markProcessedIfNew(event.eventId())) {
            log.info("Duplicate delivery of {} (eventId={}) — skipping", event.type(), event.eventId());
            return;
        }

        String sku = String.valueOf(event.payload().get("sku"));
        int quantity = toInt(event.payload().get("quantity"));

        boolean reserved = inventoryService.reserve(sku, quantity);

        OrderEvent outbound;
        if (reserved) {
            outbound = new OrderEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    OrderEvent.INVENTORY_RESERVED,
                    Map.of("sku", sku, "quantity", quantity),
                    Instant.now());
            log.info("Reserved {} units of {} for order {}", quantity, sku, event.orderId());
        } else {
            String reason = inventoryService.rejectionReason(sku, quantity);
            outbound = new OrderEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    OrderEvent.INVENTORY_REJECTED,
                    Map.of("sku", sku, "reason", reason),
                    Instant.now());
            log.info("Rejected reservation for order {}: {}", event.orderId(), reason);
        }

        kafkaTemplate.send("order-events", outbound.orderId(), outbound);
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
