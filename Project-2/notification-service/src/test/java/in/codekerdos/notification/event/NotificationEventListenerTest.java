package in.codekerdos.notification.event;

import in.codekerdos.notification.entity.NotificationLog;
import in.codekerdos.notification.repository.NotificationLogRepository;
import in.codekerdos.notification.service.IdempotencyService;
import in.codekerdos.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    IdempotencyService idempotencyService;

    @Mock
    NotificationLogRepository notificationLogRepository;

    NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        // Real NotificationService (message-building logic under test), backed by a mocked repository.
        NotificationService notificationService = new NotificationService(notificationLogRepository);
        listener = new NotificationEventListener(idempotencyService, notificationService);
    }

    private OrderEvent newEvent(String type, Map<String, Object> payload) {
        return new OrderEvent(UUID.randomUUID().toString(), "order-123", type, payload, Instant.now());
    }

    @Test
    void onOrderEvent_approved_buildsExpectedMessage() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.ORDER_APPROVED, Map.of());

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage())
                .isEqualTo("Your order #order-123 has been approved and is being processed.");
        assertThat(captor.getValue().getEventType()).isEqualTo(OrderEvent.ORDER_APPROVED);
    }

    @Test
    void onOrderEvent_held_buildsExpectedMessage() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.ORDER_HELD, Map.of());

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage())
                .isEqualTo("Your order #order-123 is on hold pending manual review.");
    }

    @Test
    void onOrderEvent_rejected_withReasoning_includesReasoningInMessage() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.ORDER_REJECTED, Map.of("reasoning", "High fraud risk detected."));

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage())
                .isEqualTo("Your order #order-123 was rejected. High fraud risk detected.");
    }

    @Test
    void onOrderEvent_rejected_withoutReasoning_omitsReasoningSuffix() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.ORDER_REJECTED, Map.of());

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Your order #order-123 was rejected.");
    }

    @Test
    void onOrderEvent_duplicateDelivery_skipsSilently() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(false);
        OrderEvent event = newEvent(OrderEvent.ORDER_APPROVED, Map.of());

        listener.onOrderEvent(event);

        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void onOrderEvent_nonTerminalType_isIgnored() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.ORDER_CREATED, Map.of("sku", "SKU-1"));

        listener.onOrderEvent(event);

        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void onOrderEvent_riskScoredType_isIgnored() {
        when(idempotencyService.markProcessedIfNew(anyString())).thenReturn(true);
        OrderEvent event = newEvent(OrderEvent.RISK_SCORED, Map.of("decision", "APPROVE"));

        listener.onOrderEvent(event);

        verify(notificationLogRepository, never()).save(any());
    }
}
