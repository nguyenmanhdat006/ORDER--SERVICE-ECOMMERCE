package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.ShippingServiceClient;
import com.ecommerce.orderservice.dto.payment.CreatePaymentRequest;
import com.ecommerce.orderservice.dto.payment.PaymentResponse;
import com.ecommerce.orderservice.dto.request.AddressRequest;
import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.OrderSearchRequest;
import com.ecommerce.orderservice.dto.request.PaymentConfirmRequest;
import com.ecommerce.orderservice.dto.request.OrderItemRequest;
import com.ecommerce.orderservice.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderSummaryResponse;
import com.ecommerce.orderservice.dto.response.PageResponse;
import com.ecommerce.orderservice.dto.shipping.CalculateFeeRequest;
import com.ecommerce.orderservice.dto.shipping.CalculateFeeResponse;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentRequest;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.enums.PaymentMethod;
import com.ecommerce.orderservice.enums.PaymentStatus;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.event.OrderEventProducer;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final OrderStatusService orderStatusService;
    private final CartServiceClient cartServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final ShippingServiceClient shippingServiceClient;
    private final OrderPostProcessService orderPostProcessService;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public OrderResponse createOrder(CreateOrderRequest request) {
        String userId = getCurrentUserId();
        try {
            String orderNumber = generateOrderNumber();
            PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .status(OrderStatus.PENDING)
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .shippingAddress(objectMapper.valueToTree(request.getShippingAddress()))
                    .billingAddress(null)
                    .customerName(request.getShippingAddress().getRecipientName())
                    .customerEmail(getCurrentUserEmail())
                    .customerPhone(request.getShippingAddress().getPhone())
                    .notes(request.getNote())
                    .orderedAt(LocalDateTime.now())
                    .build();

            BigDecimal subtotal = orderItemService.calculateItemsTotalFromRequests(request.getItems());
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal shipping = calculateShipping(request.getShippingAddress(), request.getItems(), subtotal);
            BigDecimal tax = calculateTax(subtotal);
            BigDecimal total = subtotal.subtract(discount).add(shipping).add(tax);

            order.setSubtotal(subtotal);
            order.setDiscount(discount);
            order.setShipping(shipping);
            order.setTax(tax);
            order.setTotal(total);

            Order savedOrder = orderRepository.save(order);
            List<OrderItem> createdItems = orderItemService.createOrderItemsFromRequests(savedOrder, request.getItems());
            orderStatusService.addStatusHistory(savedOrder, OrderStatus.PENDING, "Order created", "system");

            PaymentResponse payment = paymentServiceClient.createPayment(
                    CreatePaymentRequest.builder()
                            .orderId(savedOrder.getId().toString())
                            .orderNumber(savedOrder.getOrderNumber())
                            .userId(savedOrder.getUserId())
                            .amount(savedOrder.getTotal())
                            .paymentMethod(savedOrder.getPaymentMethod().name().equals("CASH_ON_DELIVERY") ? "COD" : "VNPAY")
                            .build()
            );

            savedOrder.setPaymentId(resolvePaymentId(payment));
            savedOrder.setPaymentUrl(payment.getPaymentUrl());
            Order persistedOrder = orderRepository.save(savedOrder);

            log.info("Order created: {}, items: {}", persistedOrder.getOrderNumber(), createdItems.size());
            return orderMapper.toResponse(persistedOrder);
        } catch (Exception e) {
            log.error("Error creating order", e);
            throw new BadRequestException("Failed to create order: " + e.getMessage());
        }
    }

    public OrderResponse confirmPayment(UUID id, PaymentConfirmRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.getPaymentNumber() != null && order.getPaymentId() != null
                && !request.getPaymentNumber().equals(order.getPaymentId())) {
            throw new BadRequestException("Payment does not match order");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return orderMapper.toResponse(order);
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        if (request.getPaymentNumber() != null && !request.getPaymentNumber().isBlank()) {
            order.setPaymentId(request.getPaymentNumber());
        }
        if (request.getTransactionId() != null && !request.getTransactionId().isBlank()) {
            order.setTrackingNumber(request.getTransactionId());
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
            orderStatusService.addStatusHistory(order, OrderStatus.CONFIRMED,
                    "Order confirmed from payment callback", "payment-service");
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse confirmOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return orderMapper.toResponse(order);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        orderStatusService.addStatusHistory(savedOrder, OrderStatus.CONFIRMED, "Order confirmed manually", getCurrentUserId());
        return orderMapper.toResponse(savedOrder);
    }

    public PageResponse<OrderResponse> getMyOrders(int page, int size) {
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderedAt").descending());
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);

        return PageResponse.<OrderResponse>builder()
                .content(orders.getContent().stream().map(orderMapper::toResponse).toList())
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .last(orders.isLast())
                .first(orders.isFirst())
                .numberOfElements(orders.getNumberOfElements())
                .build();
    }

    // MIGRATION: New REST API callback method (replaces Kafka events)
    // Called by Shipping Service after shipment is created
    public OrderResponse confirmShipment(UUID id, String shipmentId, String trackingNumber) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (shipmentId != null && !shipmentId.isBlank()) {
            order.setShipmentId(shipmentId);
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            order.setTrackingNumber(trackingNumber);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Shipment confirmed via REST API for order: {}, shipmentId: {}, tracking: {}", 
                order.getOrderNumber(), shipmentId, trackingNumber);
        return orderMapper.toResponse(savedOrder);
    }

    // MIGRATION: New REST API method (replaces Kafka events)
    // Called when delivery is completed
    public OrderResponse markDeliveryCompleted(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setPaymentStatus(PaymentStatus.PAID);
        Order savedOrder = orderRepository.save(order);
        
        orderStatusService.addStatusHistory(savedOrder, OrderStatus.DELIVERED, 
                "Order delivered", "shipping-service");

        try {
            paymentServiceClient.markPaymentSuccess(savedOrder.getOrderNumber());
            log.info("Updated payment status to SUCCESS for order: {}", savedOrder.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to update payment status for order: {}", savedOrder.getOrderNumber(), e);
        }

        log.info("Delivery completed for order: {}", order.getOrderNumber());
        return orderMapper.toResponse(savedOrder);
    }

    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        String userId = getCurrentUserId();
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You don't have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
    }

    public OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateStatusTransition(order.getStatus(), request.getStatus());
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (request.getStatus() == OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);
        orderStatusService.addStatusHistory(savedOrder, request.getStatus(),
                request.getNotes() != null ? request.getNotes() : "Status updated", getCurrentUserId());

        if (request.getStatus() == OrderStatus.CANCELLED && !oldStatus.equals(OrderStatus.CANCELLED)) {
            restoreProductStock(savedOrder);
        }

        publishAfterCommit(() -> orderEventProducer.publishOrderStatusChanged(savedOrder, request.getStatus(), request.getNotes()));
        return orderMapper.toResponse(savedOrder);
    }

    public void cancelOrder(UUID id, CancelOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED).contains(order.getStatus())) {
            throw new BadRequestException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        restoreProductStock(savedOrder);

        orderStatusService.addStatusHistory(savedOrder, OrderStatus.CANCELLED,
                "Cancelled: " + request.getReason(), getCurrentUserId());

        publishAfterCommit(() -> orderEventProducer.publishOrderCancelled(savedOrder, request.getReason()));
    }

    public PageResponse<OrderResponse> searchOrders(OrderSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate().atStartOfDay() : null;
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate().atTime(23, 59, 59) : null;

        Page<Order> orders = orderRepository.searchOrders(
                request.getKeyword(), request.getStatus(), startDate, endDate, pageable);

        return new PageResponse<>(
                orders.getContent().stream().map(orderMapper::toResponse).toList(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isLast(),
                orders.isFirst(),
                orders.getNumberOfElements()
        );
    }

    public OrderSummaryResponse getOrderSummary() {
        Map<String, Integer> ordersByStatus = new HashMap<>();
        long totalOrders = 0;

        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            if (count > 0) {
                ordersByStatus.put(status.toString(), Math.toIntExact(count));
                totalOrders += count;
            }
        }

        Page<Order> allOrders = orderRepository.findAll(PageRequest.of(0, Integer.MAX_VALUE));
        BigDecimal totalAmount = allOrders.getContent().stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderSummaryResponse.builder()
                .totalOrders(Math.toIntExact(totalOrders))
                .totalAmount(totalAmount)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        String date = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        long count = orderRepository.findAll().stream().filter(o -> o.getOrderNumber().contains(date)).count();
        return String.format("ORD-%s-%04d", date, count + 1);
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(0.1));
    }

    private BigDecimal calculateShipping(AddressRequest shippingAddress, List<OrderItemRequest> items, BigDecimal orderValue) {
        try {
            CalculateFeeRequest request = CalculateFeeRequest.builder()
                    .city(shippingAddress.getCity())
                    .province(shippingAddress.getProvince())
                    .weight(calculateOrderWeight(items))
                    .orderValue(orderValue)
                    .build();

            CalculateFeeResponse response = shippingServiceClient.calculateFee(request);
            return response.getShippingFee() != null ? response.getShippingFee() : BigDecimal.valueOf(10);
        } catch (Exception ex) {
            log.warn("Fallback to flat shipping fee because shipping service is unavailable");
            return BigDecimal.valueOf(10);
        }
    }

    private int calculateOrderWeight(List<OrderItemRequest> items) {
        int quantity = items.stream().map(item -> item.getQuantity() != null ? item.getQuantity() : 0).reduce(0, Integer::sum);
        return Math.max(quantity, 1) * 1000;
    }

    private String resolvePaymentId(PaymentResponse payment) {
        if (payment.getPaymentNumber() != null && !payment.getPaymentNumber().isBlank()) {
            return payment.getPaymentNumber();
        }
        return payment.getId() != null ? String.valueOf(payment.getId()) : null;
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        Map<OrderStatus, EnumSet<OrderStatus>> validTransitions = new HashMap<>();
        validTransitions.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        validTransitions.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        validTransitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        validTransitions.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));

        if (!validTransitions.getOrDefault(current, EnumSet.noneOf(OrderStatus.class)).contains(next)) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + next);
        }
    }

    private void restoreProductStock(Order order) {
        orderItemService.getOrderItems(order.getId()).forEach(item -> {
            try {
                log.info("Restoring stock for product: {} quantity: {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Error restoring stock for product: {}", item.getProductId(), e);
            }
        });
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getCurrentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
            return jwtAuth.getToken().getTokenValue();
        }
        if (auth != null && auth.getCredentials() instanceof String credentials && !credentials.isBlank()) {
            return credentials;
        }
        return "";
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }

            String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }
        return auth != null ? auth.getName() : "anonymous";
    }

    private void publishAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private PaymentMethod parsePaymentMethod(String method) {
        String normalized = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        if ("COD".equals(normalized)) {
            return PaymentMethod.CASH_ON_DELIVERY;
        }
        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment method: " + method);
        }
    }
}





