package in.codekerdos.airisk.service;

import in.codekerdos.airisk.dto.RiskDecision;
import in.codekerdos.airisk.entity.RiskDecisionLog;
import in.codekerdos.airisk.event.OrderEvent;
import in.codekerdos.airisk.repository.RiskDecisionLogRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Calls Groq (via Spring AI's ChatClient) for a forced-structured-JSON risk
 * decision on a newly created order. The call is wrapped in CircuitBreaker +
 * Retry + Bulkhead (named instance "groqRiskCall", configured in
 * application.yml) so a flaky/circuit-open Groq degrades gracefully to a safe
 * default HOLD decision instead of hard-failing the Kafka consumer — a
 * RISK_SCORED event is published either way. Only a fully-killed
 * ai-risk-service PROCESS (no consumer at all) leaves order-service's orders
 * stuck in PENDING_RISK; that is out of scope for this class to "fix" — it is
 * the deliberate Week 5 chaos-test behavior, handled by order-service's saga
 * logic.
 */
@Service
public class AiRiskService {

    private static final Logger log = LoggerFactory.getLogger(AiRiskService.class);

    private static final Set<String> ALLOWED_DECISIONS = Set.of("APPROVE", "HOLD", "REJECT");

    private static final RiskDecision FALLBACK_DECISION = new RiskDecision(
            "HOLD",
            50,
            "AI risk engine unavailable — defaulted to HOLD for manual review (circuit breaker open / Groq call failing)"
    );

    private final ChatClient chatClient;
    private final RiskDecisionLogRepository riskDecisionLogRepository;

    public AiRiskService(ChatClient chatClient, RiskDecisionLogRepository riskDecisionLogRepository) {
        this.chatClient = chatClient;
        this.riskDecisionLogRepository = riskDecisionLogRepository;
    }

    @CircuitBreaker(name = "groqRiskCall", fallbackMethod = "fallbackRiskDecision")
    @Retry(name = "groqRiskCall")
    @Bulkhead(name = "groqRiskCall")
    public RiskDecision assessRisk(OrderEvent event) {
        String promptText = buildPrompt(event);

        RiskDecision rawDecision = chatClient.prompt()
                .user(promptText)
                .call()
                .entity(RiskDecision.class);

        RiskDecision decision = validateAndClamp(rawDecision);

        persistAndLog(event.orderId(), decision);
        return decision;
    }

    /**
     * Resilience4j fallback — same signature as assessRisk plus a trailing
     * Throwable. Invoked when the circuit is open, retries are exhausted, or
     * the bulkhead rejects the call. Still results in an auditable decision:
     * persisted to the DB and logged, same as the happy path, so a flaky Groq
     * never silently drops an order's risk assessment.
     */
    public RiskDecision fallbackRiskDecision(OrderEvent event, Throwable t) {
        log.warn("Groq risk call failed for order {} — falling back to safe default HOLD. Cause: {}",
                event.orderId(), t.toString());
        persistAndLog(event.orderId(), FALLBACK_DECISION);
        return FALLBACK_DECISION;
    }

    String buildPrompt(OrderEvent event) {
        var payload = event.payload() != null ? event.payload() : java.util.Map.<String, Object>of();
        Object sku = payload.get("sku");
        Object quantity = payload.get("quantity");
        Object totalAmount = payload.get("totalAmount");
        Object customerEmail = payload.get("customerEmail");

        return """
                You are a fraud/risk analyst for an e-commerce order platform.
                Assess the risk of the following order and decide whether it should be
                approved, held for manual review, or rejected.

                Order details:
                - SKU: %s
                - Quantity: %s
                - Total amount: %s
                - Customer email: %s

                Respond with ONLY a JSON object matching this exact shape, no extra text:
                {"decision": "APPROVE|HOLD|REJECT", "riskScore": <integer 0-100>, "reasoning": "<1-2 sentence explanation>"}

                The "decision" field MUST be exactly one of: APPROVE, HOLD, REJECT.
                """.formatted(
                String.valueOf(sku),
                String.valueOf(quantity),
                String.valueOf(totalAmount),
                String.valueOf(customerEmail)
        );
    }

    /** Clamps an out-of-range/invalid decision from the model to HOLD, appending a note to the reasoning. */
    RiskDecision validateAndClamp(RiskDecision decision) {
        if (decision == null) {
            return new RiskDecision("HOLD", 50,
                    "AI risk engine returned an empty response — defaulted to HOLD for manual review.");
        }
        if (decision.decision() == null || !ALLOWED_DECISIONS.contains(decision.decision())) {
            String note = "AI risk engine returned an invalid decision value ('" + decision.decision()
                    + "') — clamped to HOLD for manual review.";
            String reasoning = decision.reasoning() != null ? decision.reasoning() + " " + note : note;
            return new RiskDecision("HOLD", decision.riskScore(), reasoning);
        }
        return decision;
    }

    private void persistAndLog(String orderId, RiskDecision decision) {
        RiskDecisionLog logRow = new RiskDecisionLog(orderId, decision.decision(), decision.riskScore(), decision.reasoning());
        riskDecisionLogRepository.save(logRow);
        log.info("Risk decision for order {}: {} (score={}) — {}",
                orderId, decision.decision(), decision.riskScore(), decision.reasoning());
    }
}
