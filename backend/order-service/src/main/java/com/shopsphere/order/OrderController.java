package com.shopsphere.order;

import com.shopsphere.order.OrderApi.CreateOrderRequest;
import com.shopsphere.order.OrderApi.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orders;
    private final OrderService service;
    public OrderController(OrderRepository orders, OrderService service) { this.orders = orders; this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(service.create(userId, request));
    }
    @GetMapping
    public List<OrderResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return orders.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return OrderResponse.from(orders.findWithItemsById(id).filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    @GetMapping("/user/{userId}")
    public List<OrderResponse> byUser(@PathVariable Long userId, @RequestHeader("X-User-Id") Long currentUser,
                                      @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role) && !userId.equals(currentUser)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return orders.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }
    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        OrderEntity order = orders.findWithItemsById(id).filter(value -> value.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING) throw new ResponseStatusException(HttpStatus.CONFLICT, "Order can no longer be cancelled");
        order.cancel();
        return OrderResponse.from(orders.save(order));
    }

    @PutMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable Long id, @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return OrderResponse.from(service.ship(id));
    }
}