package in.codekerdos.airisk.event;

import in.codekerdos.airisk.dto.RiskDecision;
import in.codekerdos.airisk.service.AiRiskService;
import in.codekerdos.airisk.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Reacts ONLY to ORDER_CREATED on "order-events" — asks AiRiskService for a
 * Groq-backed (or fallback) risk decision, then publishes RISK_SCORED for
 * order-service to fold back into the order's saga state. All other event
 * types on the topic are intentionally ignored — they're signals other
 * services own.
 */
@Component
public class AiRiskEventListener {

    private static final Logger log = LoggerFactory.getLogger(AiRiskEventListener.class);

    private final IdempotencyService idempotencyService;
    private final AiRiskService aiRiskService;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public AiRiskEventListener(IdempotencyService idempotencyService,
                                AiRiskService aiRiskService,
                                KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.idempotencyService = idempotencyService;
        this.aiRiskService = aiRiskService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-events", groupId = "ai-risk-service")
    public void onOrderEvent(OrderEvent event) {
        if (!OrderEvent.ORDER_CREATED.equals(event.type())) {
            log.debug("Ignoring event {} (eventId={}) for order {}", event.type(), event.eventId(), event.orderId());
            return;
        }

        if (!idempotencyService.markProcessedIfNew(event.eventId())) {
            log.info("Duplicate delivery of {} (eventId={}) — skipping", event.type(), event.eventId());
            return;
        }

        RiskDecision decision = aiRiskService.assessRisk(event);

        OrderEvent riskScoredEvent = new OrderEvent(
                UUID.randomUUID().toString(),
                event.orderId(),
                OrderEvent.RISK_SCORED,
                Map.of(
                        "decision", decision.decision(),
                        "riskScore", decision.riskScore(),
                        "reasoning", decision.reasoning() != null ? decision.reasoning() : ""
                ),
                Instant.now()
        );

        kafkaTemplate.send("order-events", riskScoredEvent.orderId(), riskScoredEvent);
        log.info("Published RISK_SCORED for order {}: {}", event.orderId(), decision.decision());
    }
}
