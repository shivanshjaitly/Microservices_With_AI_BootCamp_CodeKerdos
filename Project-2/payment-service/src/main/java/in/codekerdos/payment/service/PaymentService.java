package in.codekerdos.payment.service;

import in.codekerdos.payment.entity.Payment;
import in.codekerdos.payment.entity.PaymentStatus;
import in.codekerdos.payment.exception.ResourceNotFoundException;
import in.codekerdos.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;

/**
 * Simulates a payment gateway charge. Demoable, deterministic-ish failure
 * rule for teaching:
 * <ol>
 *   <li>If the order's {@code totalAmount} exceeds
 *       {@code app.payment.max-auto-approve-amount} (default 5000), the
 *       charge is ALWAYS declined — "amount too large to auto-approve".
 *       This is the deterministic path: order a laptop over the threshold
 *       in class and PAYMENT_FAILED fires every time.</li>
 *   <li>Otherwise, there's a small random decline chance
 *       ({@code app.payment.random-failure-rate}, default 0.10 = 10%) to
 *       simulate a flaky real-world payment gateway.</li>
 * </ol>
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BigDecimal maxAutoApproveAmount;
    private final double randomFailureRate;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository,
                           @Value("${app.payment.max-auto-approve-amount:5000}") BigDecimal maxAutoApproveAmount,
                           @Value("${app.payment.random-failure-rate:0.10}") double randomFailureRate) {
        this.paymentRepository = paymentRepository;
        this.maxAutoApproveAmount = maxAutoApproveAmount;
        this.randomFailureRate = randomFailureRate;
    }

    public record PaymentOutcome(Payment payment, String failureReason) {
    }

    @Transactional
    public PaymentOutcome charge(String orderId, BigDecimal amount) {
        ChargeDecision decision = decide(amount);
        Payment payment = new Payment(
                orderId,
                amount,
                decision.approved() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED,
                Instant.now());
        paymentRepository.save(payment);
        return new PaymentOutcome(payment, decision.reason());
    }

    public Payment getByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order " + orderId));
    }

    private record ChargeDecision(boolean approved, String reason) {
    }

    private ChargeDecision decide(BigDecimal amount) {
        if (amount.compareTo(maxAutoApproveAmount) > 0) {
            return new ChargeDecision(false, "Amount " + amount
                    + " exceeds max auto-approve threshold of " + maxAutoApproveAmount);
        }
        if (random.nextDouble() < randomFailureRate) {
            return new ChargeDecision(false, "Simulated payment gateway decline (random failure rate "
                    + randomFailureRate + ")");
        }
        return new ChargeDecision(true, null);
    }
}
