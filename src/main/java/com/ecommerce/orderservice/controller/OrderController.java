package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.OrderFilterParams;
import com.ecommerce.orderservice.dto.request.PaymentConfirmRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.PageResponse;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating order with payment method: {}", request.getPaymentMethod());
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(201).body(order);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        log.info("Confirming order: {}", id);
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody com.ecommerce.orderservice.dto.request.UpdateOrderStatusRequest request) {
        log.info("Updating order status for {}: {}", id, request.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @PutMapping("/{id}/payment-confirmed")
    public ResponseEntity<OrderResponse> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentConfirmRequest request) {
        log.info("Payment callback received for order: {}", id);
        return ResponseEntity.ok(orderService.confirmPayment(id, request));
    }

    @PutMapping("/{id}/delivered")
    public ResponseEntity<OrderResponse> markDeliveryCompleted(@PathVariable UUID id) {
        log.info("Marking delivery completed for order: {}", id);
        return ResponseEntity.ok(orderService.markDeliveryCompleted(id));
    }

    @PutMapping("/{id}/shipping-status")
    public ResponseEntity<OrderResponse> updateShippingStatus(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload) {
        String status = payload.get("status");
        log.info("Updating shipping status for order: {} to {}", id, status);
        return ResponseEntity.ok(orderService.updateShippingStatus(id, status));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @ParameterObject OrderFilterParams filterParams) {
        log.info("Fetching all orders for admin, page={}, size={}", filterParams.getPage(), filterParams.getSize());
        return ResponseEntity.ok(orderService.getAllOrders(filterParams));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching orders for current user, page={}, size={}", page, size);
        return ResponseEntity.ok(orderService.getMyOrders(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        log.info("Fetching order: {}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}

