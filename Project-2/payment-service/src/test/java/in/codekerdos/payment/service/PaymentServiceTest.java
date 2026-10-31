package in.codekerdos.payment.service;

import in.codekerdos.payment.entity.Payment;
import in.codekerdos.payment.entity.PaymentStatus;
import in.codekerdos.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void charge_alwaysDeclines_whenAmountExceedsAutoApproveThreshold() {
        // random-failure-rate 0.0 isolates the deterministic threshold rule from the random rule.
        PaymentService paymentService = new PaymentService(paymentRepository, new BigDecimal("5000"), 0.0);

        PaymentService.PaymentOutcome outcome = paymentService.charge("order-1", new BigDecimal("5000.01"));

        assertThat(outcome.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.failureReason()).contains("exceeds max auto-approve threshold");
    }

    @Test
    void charge_alwaysApproves_whenUnderThresholdAndRandomFailureRateIsZero() {
        PaymentService paymentService = new PaymentService(paymentRepository, new BigDecimal("5000"), 0.0);

        PaymentService.PaymentOutcome outcome = paymentService.charge("order-2", new BigDecimal("1200"));

        assertThat(outcome.payment().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(outcome.failureReason()).isNull();
    }

    @Test
    void charge_alwaysDeclines_whenUnderThresholdButRandomFailureRateIsOne() {
        PaymentService paymentService = new PaymentService(paymentRepository, new BigDecimal("5000"), 1.0);

        PaymentService.PaymentOutcome outcome = paymentService.charge("order-3", new BigDecimal("100"));

        assertThat(outcome.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outcome.failureReason()).contains("Simulated payment gateway decline");
    }
}
