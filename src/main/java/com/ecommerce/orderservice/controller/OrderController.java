package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.PaymentConfirmRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        log.info("Fetching order: {}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}

