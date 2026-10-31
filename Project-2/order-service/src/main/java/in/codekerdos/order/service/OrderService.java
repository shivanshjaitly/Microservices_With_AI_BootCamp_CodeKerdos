package in.codekerdos.order.service;

import in.codekerdos.order.dto.CreateOrderRequest;
import in.codekerdos.order.dto.OrderResponse;
import in.codekerdos.order.entity.Order;
import in.codekerdos.order.enums.OrderStatus;
import in.codekerdos.order.enums.SignalStatus;
import in.codekerdos.order.event.OrderEvent;
import in.codekerdos.order.event.OrderEventProducer;
import in.codekerdos.order.exception.ResourceNotFoundException;
import in.codekerdos.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Owns the order aggregate AND orchestrates the order saga: ORDER_CREATED is
 * published on create, then as INVENTORY_*, PAYMENT_*, and RISK_SCORED events
 * arrive (via OrderEventListener) this service updates the corresponding
 * signal and calls {@link #evaluateOrder(Order)} to see whether the saga can
 * conclude. The order simply stays PENDING_RISK — "queues instead of
 * failing" — until all three signals are in.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String customerEmail) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerEmail(customerEmail);
        order.setSku(request.sku());
        order.setQuantity(request.quantity());
        order.setTotalAmount(request.totalAmount());
        order.setStatus(OrderStatus.PENDING_RISK);
        order.setInventoryStatus(SignalStatus.PENDING);
        order.setPaymentStatus(SignalStatus.PENDING);
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        orderRepository.save(order);
        eventProducer.publishOrderCreated(order);
        return OrderResponse.from(order);
    }

    public OrderResponse getOrderById(String id) {
        return OrderResponse.from(findOrderOrThrow(id));
    }

    public List<OrderResponse> getOrdersByCustomer(String customerEmail) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public void onInventoryReserved(String orderId) {
        applySignal(orderId, order -> order.setInventoryStatus(SignalStatus.OK));
    }

    @Transactional
    public void onInventoryRejected(String orderId) {
        applySignal(orderId, order -> order.setInventoryStatus(SignalStatus.REJECTED));
    }

    @Transactional
    public void onPaymentCompleted(String orderId) {
        applySignal(orderId, order -> order.setPaymentStatus(SignalStatus.OK));
    }

    @Transactional
    public void onPaymentFailed(String orderId) {
        applySignal(orderId, order -> order.setPaymentStatus(SignalStatus.REJECTED));
    }

    @Transactional
    public void onRiskScored(String orderId, String decision, Integer riskScore, String reasoning) {
        applySignal(orderId, order -> {
            order.setRiskDecision(decision);
            order.setRiskScore(riskScore);
            order.setRiskReasoning(reasoning);
        });
    }

    private void applySignal(String orderId, Consumer<Order> mutator) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            mutator.accept(order);
            order.setUpdatedAt(Instant.now());
            String terminalEventType = evaluateOrder(order);
            orderRepository.save(order);
            if (terminalEventType != null) {
                eventProducer.publishTerminalEvent(order, terminalEventType);
            }
        }, () -> log.warn("Received a saga signal for unknown orderId={} — ignoring", orderId));
    }

    /**
     * Pure saga transition logic — no I/O, easy to unit test in isolation.
     * Mutates {@code order.status} in place and returns the terminal Kafka
     * event type to publish (ORDER_APPROVED/ORDER_HELD/ORDER_REJECTED), or
     * {@code null} when the order should stay PENDING_RISK (not all three
     * signals have reported yet, or the order is already terminal).
     */
    String evaluateOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_RISK) {
            return null;
        }
        boolean allSignalsIn = order.getInventoryStatus() != SignalStatus.PENDING
                && order.getPaymentStatus() != SignalStatus.PENDING
                && order.getRiskDecision() != null;
        if (!allSignalsIn) {
            return null;
        }

        OrderStatus finalStatus;
        String eventType;
        if (order.getInventoryStatus() == SignalStatus.REJECTED || order.getPaymentStatus() == SignalStatus.REJECTED) {
            finalStatus = OrderStatus.REJECTED;
            eventType = OrderEvent.ORDER_REJECTED;
        } else if ("REJECT".equals(order.getRiskDecision())) {
            finalStatus = OrderStatus.REJECTED;
            eventType = OrderEvent.ORDER_REJECTED;
        } else if ("HOLD".equals(order.getRiskDecision())) {
            finalStatus = OrderStatus.HELD;
            eventType = OrderEvent.ORDER_HELD;
        } else {
            finalStatus = OrderStatus.APPROVED;
            eventType = OrderEvent.ORDER_APPROVED;
        }

        order.setStatus(finalStatus);
        return eventType;
    }

    private Order findOrderOrThrow(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
