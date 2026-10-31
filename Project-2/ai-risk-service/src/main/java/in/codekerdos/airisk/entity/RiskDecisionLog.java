package in.codekerdos.airisk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit trail row for every risk decision this service makes — both the
 * "happy path" (a real Groq call succeeded) and the fallback path (circuit
 * open / Groq call failing, defaulted to HOLD). The reasoning text is always
 * persisted here AND logged at INFO level, so the audit trail exists even if
 * log aggregation is unavailable.
 */
@Entity
@Table(name = "risk_decisions")
public class RiskDecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String decision;

    @Column(nullable = false)
    private int riskScore;

    @Lob
    @Column(length = 2000)
    private String reasoning;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RiskDecisionLog() {
    }

    public RiskDecisionLog(String orderId, String decision, int riskScore, String reasoning) {
        this.orderId = orderId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.reasoning = reasoning;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
