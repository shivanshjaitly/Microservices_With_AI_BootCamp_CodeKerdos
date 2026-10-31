package in.codekerdos.order.service;

import in.codekerdos.order.entity.Order;
import in.codekerdos.order.enums.OrderStatus;
import in.codekerdos.order.enums.SignalStatus;
import in.codekerdos.order.event.OrderEvent;
import in.codekerdos.order.event.OrderEventProducer;
import in.codekerdos.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the pure saga transition logic in
 * {@link OrderService#evaluateOrder(Order)} — no Spring context, no DB, no
 * Kafka. Mirrors the reference bootcamp repo's BookingServiceTest style
 * (Mockito @InjectMocks with unused collaborator mocks).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderEventProducer eventProducer;

    @InjectMocks
    OrderService orderService;

    private Order pendingOrder() {
        Order order = new Order();
        order.setId("order-1");
        order.setCustomerEmail("customer@codekerdos.in");
        order.setStatus(OrderStatus.PENDING_RISK);
        order.setInventoryStatus(SignalStatus.PENDING);
        order.setPaymentStatus(SignalStatus.PENDING);
        return order;
    }

    @Test
    void evaluateOrder_allSignalsOk_approvesOrder() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.OK);
        order.setRiskDecision("APPROVE");
        order.setRiskScore(10);
        order.setRiskReasoning("low risk");

        String eventType = orderService.evaluateOrder(order);

        assertEquals(OrderEvent.ORDER_APPROVED, eventType);
        assertEquals(OrderStatus.APPROVED, order.getStatus());
    }

    @Test
    void evaluateOrder_inventoryRejected_rejectsRegardlessOfOtherSignals() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.REJECTED);
        order.setPaymentStatus(SignalStatus.OK);
        order.setRiskDecision("APPROVE");
        order.setRiskScore(5);
        order.setRiskReasoning("looked fine");

        String eventType = orderService.evaluateOrder(order);

        assertEquals(OrderEvent.ORDER_REJECTED, eventType);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void evaluateOrder_paymentRejected_rejectsRegardlessOfRiskDecision() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.REJECTED);
        order.setRiskDecision("APPROVE");
        order.setRiskScore(5);
        order.setRiskReasoning("looked fine");

        String eventType = orderService.evaluateOrder(order);

        assertEquals(OrderEvent.ORDER_REJECTED, eventType);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void evaluateOrder_riskHoldWithCleanInventoryAndPayment_holdsOrder() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.OK);
        order.setRiskDecision("HOLD");
        order.setRiskScore(55);
        order.setRiskReasoning("borderline, needs manual review");

        String eventType = orderService.evaluateOrder(order);

        assertEquals(OrderEvent.ORDER_HELD, eventType);
        assertEquals(OrderStatus.HELD, order.getStatus());
    }

    @Test
    void evaluateOrder_riskReject_rejectsOrder() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.OK);
        order.setRiskDecision("REJECT");
        order.setRiskScore(95);
        order.setRiskReasoning("fraud signals");

        String eventType = orderService.evaluateOrder(order);

        assertEquals(OrderEvent.ORDER_REJECTED, eventType);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void evaluateOrder_onlyTwoOfThreeSignalsIn_staysPendingRiskAndPublishesNoEvent() {
        Order order = pendingOrder();
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.OK);
        // riskDecision left null — RISK_SCORED has not arrived yet.

        String eventType = orderService.evaluateOrder(order);

        assertNull(eventType);
        assertEquals(OrderStatus.PENDING_RISK, order.getStatus());
    }

    @Test
    void evaluateOrder_alreadyTerminal_isNoOp() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.APPROVED);
        order.setInventoryStatus(SignalStatus.OK);
        order.setPaymentStatus(SignalStatus.OK);
        order.setRiskDecision("APPROVE");

        String eventType = orderService.evaluateOrder(order);

        assertNull(eventType);
        assertEquals(OrderStatus.APPROVED, order.getStatus());
    }
}
