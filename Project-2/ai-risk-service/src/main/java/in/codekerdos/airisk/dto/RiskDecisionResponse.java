package in.codekerdos.airisk.dto;

import in.codekerdos.airisk.entity.RiskDecisionLog;

import java.time.Instant;

public record RiskDecisionResponse(
        Long id,
        String orderId,
        String decision,
        int riskScore,
        String reasoning,
        Instant createdAt
) {
    public static RiskDecisionResponse from(RiskDecisionLog log) {
        return new RiskDecisionResponse(
                log.getId(),
                log.getOrderId(),
                log.getDecision(),
                log.getRiskScore(),
                log.getReasoning(),
                log.getCreatedAt()
        );
    }
}
