package in.codekerdos.order.entity;

import in.codekerdos.order.enums.OrderStatus;
import in.codekerdos.order.enums.SignalStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The order aggregate + saga state. {@code id} is an app-assigned UUID (not
 * DB-generated) so it can be referenced as {@code orderId} in Kafka events
 * before the row is even committed. {@code inventoryStatus}/{@code
 * paymentStatus} track the two boolean-ish signals of the saga; {@code
 * riskDecision}/{@code riskScore}/{@code riskReasoning} stay null until the
 * RISK_SCORED event arrives — see OrderService#evaluateOrder for the
 * transition logic that reads all five of these fields.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalStatus inventoryStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalStatus paymentStatus;

    /** One of APPROVE / HOLD / REJECT once RISK_SCORED has arrived; null until then. */
    private String riskDecision;

    private Integer riskScore;

    @Column(length = 1000)
    private String riskReasoning;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public SignalStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(SignalStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public SignalStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(SignalStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getRiskDecision() {
        return riskDecision;
    }

    public void setRiskDecision(String riskDecision) {
        this.riskDecision = riskDecision;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskReasoning() {
        return riskReasoning;
    }

    public void setRiskReasoning(String riskReasoning) {
        this.riskReasoning = riskReasoning;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
