package in.codekerdos.payment.dto;

import in.codekerdos.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant createdAt
) {
}
