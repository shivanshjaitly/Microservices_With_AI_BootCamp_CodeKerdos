package in.codekerdos.payment.event;

import in.codekerdos.payment.entity.PaymentStatus;
import in.codekerdos.payment.service.IdempotencyService;
import in.codekerdos.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Reacts to order-events. This service's own consumer group ("payment-service")
 * receives every event on the topic, so it must ignore anything that isn't
 * ORDER_CREATED (it will also see its own PAYMENT_COMPLETED/PAYMENT_FAILED
 * echoes, plus every other service's events).
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public PaymentEventListener(IdempotencyService idempotencyService,
                                 PaymentService paymentService,
                                 KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void onOrderEvent(OrderEvent event) {
        if (!OrderEvent.ORDER_CREATED.equals(event.type())) {
            return; // not our concern — every other event type is ignored
        }

        if (!idempotencyService.markProcessedIfNew(event.eventId())) {
            log.info("Duplicate delivery of {} (eventId={}) — skipping", event.type(), event.eventId());
            return;
        }

        BigDecimal totalAmount = toBigDecimal(event.payload().get("totalAmount"));
        PaymentService.PaymentOutcome outcome = paymentService.charge(event.orderId(), totalAmount);

        OrderEvent outbound;
        if (outcome.payment().getStatus() == PaymentStatus.COMPLETED) {
            outbound = new OrderEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    OrderEvent.PAYMENT_COMPLETED,
                    Map.of("orderId", event.orderId(), "amountCharged", totalAmount),
                    Instant.now());
            log.info("Charged {} for order {}", totalAmount, event.orderId());
        } else {
            outbound = new OrderEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    OrderEvent.PAYMENT_FAILED,
                    Map.of("orderId", event.orderId(), "reason", outcome.failureReason()),
                    Instant.now());
            log.info("Payment failed for order {}: {}", event.orderId(), outcome.failureReason());
        }

        kafkaTemplate.send("order-events", outbound.orderId(), outbound);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
