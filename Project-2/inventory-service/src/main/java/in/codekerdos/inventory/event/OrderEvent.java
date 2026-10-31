package in.codekerdos.inventory.event;

import java.time.Instant;
import java.util.Map;

/**
 * Wire contract for the shared Kafka topic "order-events". Every service in
 * Project 2 owns its own copy of this shape — services are decoupled at the
 * JSON contract, not via a shared JAR (same pattern as week-06/week-07 in the
 * reference bootcamp repo).
 */
public record OrderEvent(
        String eventId,
        String orderId,
        String type,
        Map<String, Object> payload,
        Instant timestamp
) {
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_REJECTED = "INVENTORY_REJECTED";
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String RISK_SCORED = "RISK_SCORED";
    public static final String ORDER_APPROVED = "ORDER_APPROVED";
    public static final String ORDER_HELD = "ORDER_HELD";
    public static final String ORDER_REJECTED = "ORDER_REJECTED";
}
