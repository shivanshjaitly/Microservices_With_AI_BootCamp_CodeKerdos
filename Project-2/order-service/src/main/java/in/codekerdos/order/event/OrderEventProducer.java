package in.codekerdos.order.event;

import in.codekerdos.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes to the shared Kafka topic "order-events". Every event gets a
 * fresh eventId (it identifies the event, not the order) and is keyed by
 * orderId so all events for one order land on the same partition and stay
 * ordered.
 */
@Component
public class OrderEventProducer {

    private static final String TOPIC = "order-events";
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Fired the instant an order is created, before inventory/payment/risk have weighed in. */
    public void publishOrderCreated(Order order) {
        Map<String, Object> payload = Map.of(
                "sku", order.getSku(),
                "quantity", order.getQuantity(),
                "totalAmount", order.getTotalAmount(),
                "customerEmail", order.getCustomerEmail()
        );
        publish(order.getId(), OrderEvent.ORDER_CREATED, payload);
    }

    /**
     * Fired by OrderService#evaluateOrder once all three saga signals
     * (inventory, payment, risk) have reported and a final status has been
     * computed — type is one of ORDER_APPROVED / ORDER_HELD / ORDER_REJECTED.
     * notification-service consumes this to notify the customer.
     */
    public void publishTerminalEvent(Order order, String type) {
        Map<String, Object> payload = Map.of(
                "status", String.valueOf(order.getStatus()),
                "inventoryStatus", String.valueOf(order.getInventoryStatus()),
                "paymentStatus", String.valueOf(order.getPaymentStatus()),
                "riskDecision", String.valueOf(order.getRiskDecision()),
                "riskScore", String.valueOf(order.getRiskScore()),
                "riskReasoning", String.valueOf(order.getRiskReasoning())
        );
        publish(order.getId(), type, payload);
    }

    private void publish(String orderId, String type, Map<String, Object> payload) {
        OrderEvent event = new OrderEvent(
                UUID.randomUUID().toString(),
                orderId,
                type,
                payload,
                Instant.now()
        );
        log.info("Publishing {} for orderId={} eventId={}", type, orderId, event.eventId());
        kafkaTemplate.send(TOPIC, event.orderId(), event);
    }
}
