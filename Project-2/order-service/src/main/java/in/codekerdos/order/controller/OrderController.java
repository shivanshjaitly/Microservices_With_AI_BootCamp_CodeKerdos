package in.codekerdos.order.controller;

import in.codekerdos.order.dto.CreateOrderRequest;
import in.codekerdos.order.dto.OrderResponse;
import in.codekerdos.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Place an order — persists PENDING_RISK and publishes ORDER_CREATED")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                 Authentication authentication) {
        OrderResponse response = orderService.createOrder(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one order — owning customer or an ADMIN only")
    public ResponseEntity<OrderResponse> getById(@PathVariable String id, Authentication authentication) {
        OrderResponse order = orderService.getOrderById(id);
        if (!isAdmin(authentication) && !order.customerEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("You may only view your own orders");
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List the calling customer's own orders")
    public ResponseEntity<List<OrderResponse>> mine(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List every order in the system")
    public ResponseEntity<List<OrderResponse>> all() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
