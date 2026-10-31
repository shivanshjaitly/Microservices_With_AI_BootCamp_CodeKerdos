package in.codekerdos.order.dto;

import in.codekerdos.order.entity.Order;
import in.codekerdos.order.enums.OrderStatus;
import in.codekerdos.order.enums.SignalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String customerEmail,
        String sku,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        SignalStatus inventoryStatus,
        SignalStatus paymentStatus,
        String riskDecision,
        Integer riskScore,
        String riskReasoning,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getSku(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getInventoryStatus(),
                order.getPaymentStatus(),
                order.getRiskDecision(),
                order.getRiskScore(),
                order.getRiskReasoning(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
