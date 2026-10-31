package in.codekerdos.airisk.service;

import in.codekerdos.airisk.dto.RiskDecision;
import in.codekerdos.airisk.entity.RiskDecisionLog;
import in.codekerdos.airisk.event.OrderEvent;
import in.codekerdos.airisk.repository.RiskDecisionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRiskServiceTest {

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    RiskDecisionLogRepository riskDecisionLogRepository;

    AiRiskService aiRiskService;

    @BeforeEach
    void setUp() {
        aiRiskService = new AiRiskService(chatClient, riskDecisionLogRepository);
    }

    private OrderEvent orderCreatedEvent() {
        return new OrderEvent(
                UUID.randomUUID().toString(),
                "order-42",
                OrderEvent.ORDER_CREATED,
                Map.of(
                        "sku", "SKU-100",
                        "quantity", 3,
                        "totalAmount", "149.99",
                        "customerEmail", "buyer@codekerdos.in"
                ),
                Instant.now()
        );
    }

    @Test
    void assessRisk_successfulGroqCall_returnsModelDecisionAndPersistsLog() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(RiskDecision.class)))
                .thenReturn(new RiskDecision("APPROVE", 12, "Low value order, trusted customer domain."));

        RiskDecision result = aiRiskService.assessRisk(orderCreatedEvent());

        assertThat(result.decision()).isEqualTo("APPROVE");
        assertThat(result.riskScore()).isEqualTo(12);
        assertThat(result.reasoning()).isEqualTo("Low value order, trusted customer domain.");

        ArgumentCaptor<RiskDecisionLog> captor = ArgumentCaptor.forClass(RiskDecisionLog.class);
        verify(riskDecisionLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("order-42");
        assertThat(captor.getValue().getDecision()).isEqualTo("APPROVE");
        assertThat(captor.getValue().getRiskScore()).isEqualTo(12);
    }

    @Test
    void assessRisk_modelReturnsInvalidDecision_clampsToHold() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(RiskDecision.class)))
                .thenReturn(new RiskDecision("MAYBE", 40, "Unsure about this one."));

        RiskDecision result = aiRiskService.assessRisk(orderCreatedEvent());

        assertThat(result.decision()).isEqualTo("HOLD");
        assertThat(result.reasoning()).contains("Unsure about this one.")
                .contains("clamped to HOLD");

        ArgumentCaptor<RiskDecisionLog> captor = ArgumentCaptor.forClass(RiskDecisionLog.class);
        verify(riskDecisionLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("HOLD");
    }

    @Test
    void validateAndClamp_directCall_nullDecision_defaultsToHold() {
        RiskDecision clamped = aiRiskService.validateAndClamp(null);

        assertThat(clamped.decision()).isEqualTo("HOLD");
        assertThat(clamped.riskScore()).isEqualTo(50);
    }

    @Test
    void fallbackRiskDecision_returnsSafeDefaultHoldAndPersistsAuditableLog() {
        OrderEvent event = orderCreatedEvent();

        RiskDecision fallback = aiRiskService.fallbackRiskDecision(event, new RuntimeException("circuit open"));

        assertThat(fallback.decision()).isEqualTo("HOLD");
        assertThat(fallback.riskScore()).isEqualTo(50);
        assertThat(fallback.reasoning())
                .isEqualTo("AI risk engine unavailable — defaulted to HOLD for manual review (circuit breaker open / Groq call failing)");

        ArgumentCaptor<RiskDecisionLog> captor = ArgumentCaptor.forClass(RiskDecisionLog.class);
        verify(riskDecisionLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("order-42");
        assertThat(captor.getValue().getDecision()).isEqualTo("HOLD");
        assertThat(captor.getValue().getReasoning()).isEqualTo(fallback.reasoning());
    }
}
