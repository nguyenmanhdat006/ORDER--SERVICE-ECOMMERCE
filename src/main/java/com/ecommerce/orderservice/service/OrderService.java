package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.OrderSearchRequest;
import com.ecommerce.orderservice.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderSummaryResponse;
import com.ecommerce.orderservice.dto.response.PageResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
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
import java.util.Map;
import java.util.UUID;

/**
 * Order Service
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final OrderStatusService orderStatusService;
    private final CartServiceClient cartServiceClient;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    /**
     * Create order from cart
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        String userId = getCurrentUserId();
        String token = getCurrentToken();

        log.info("Preparing Cart Service call for createOrder: userId={}, authTokenPresent={}",
                userId, token != null && !token.isBlank());

        // Get cart from cart service
        CartServiceClient.CartResponse cart = cartServiceClient.getCart(userId, token);

        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        try {
            // Generate order number
            String orderNumber = generateOrderNumber();

            // Create Order entity
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .status(OrderStatus.PENDING)
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentMethod(parsePaymentMethod(request.getPaymentMethod()))
                    .shippingAddress(objectMapper.valueToTree(request.getShippingAddress()))
                    .billingAddress(request.getBillingAddress() != null ?
                            objectMapper.valueToTree(request.getBillingAddress()) :
                            objectMapper.valueToTree(request.getShippingAddress()))
                    .customerName(request.getShippingAddress().getFullName())
                    .customerEmail(getCurrentUserEmail())
                    .customerPhone(request.getShippingAddress().getPhone())
                    .notes(request.getNotes())
                    .orderedAt(LocalDateTime.now())
                    .build();

            // Calculate totals
            BigDecimal subtotal = orderItemService.calculateItemsTotal(cart.items);
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal shipping = calculateShipping();
            BigDecimal tax = calculateTax(subtotal);
            BigDecimal total = subtotal.subtract(discount).add(shipping).add(tax);

            order.setSubtotal(subtotal);
            order.setDiscount(discount);
            order.setShipping(shipping);
            order.setTax(tax);
            order.setTotal(total);

            // Save order
                  Order savedOrder = orderRepository.save(order);

            // Create order items after order is managed/persisted
                  List<OrderItem> createdItems = orderItemService.createOrderItems(savedOrder, cart.items);

            // Add status history
                  orderStatusService.addStatusHistory(savedOrder, OrderStatus.PENDING, "Order created", "system");


                  publishAfterCommit(() -> orderEventProducer.publishOrderCreated(savedOrder, createdItems));

            log.info("Order created successfully: {}", orderNumber);
                  return orderMapper.toResponse(savedOrder);

        } catch (Exception e) {
            log.error("Error creating order: ", e);
            throw new BadRequestException("Failed to create order: " + e.getMessage());
        }
    }

    /**
     * Get current user's orders (paginated)
     */
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

    /**
     * Get order by ID
     */
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        String userId = getCurrentUserId();
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You don't have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    /**
     * Get order by order number
     */
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return orderMapper.toResponse(order);
    }

    /**
     * Update order status
     */
    public OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Validate status transition
        validateStatusTransition(order.getStatus(), request.getStatus());

        // Update order status
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        // Update timestamp based on status
        if (request.getStatus() == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (request.getStatus() == OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

          Order savedOrder = orderRepository.save(order);

        // Add status history
          orderStatusService.addStatusHistory(savedOrder, request.getStatus(),
                request.getNotes() != null ? request.getNotes() : "Status updated", getCurrentUserId());

        // Restore stock if cancelled
        if (request.getStatus() == OrderStatus.CANCELLED && !oldStatus.equals(OrderStatus.CANCELLED)) {
            restoreProductStock(savedOrder);
        }

          publishAfterCommit(() -> orderEventProducer.publishOrderStatusChanged(savedOrder, request.getStatus(), request.getNotes()));

        log.info("Order status updated: {} -> {}", oldStatus, request.getStatus());
          return orderMapper.toResponse(savedOrder);
    }

    /**
     * Cancel order
     */
    public void cancelOrder(UUID id, CancelOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Validate order can be cancelled
        if (!EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED).contains(order.getStatus())) {
            throw new BadRequestException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        // Update status to cancelled
        order.setStatus(OrderStatus.CANCELLED);
          Order savedOrder = orderRepository.save(order);

        // Restore stock
          restoreProductStock(savedOrder);

        // Add status history
          orderStatusService.addStatusHistory(savedOrder, OrderStatus.CANCELLED,
                "Cancelled: " + request.getReason(), getCurrentUserId());

          publishAfterCommit(() -> orderEventProducer.publishOrderCancelled(savedOrder, request.getReason()));

        log.info("Order cancelled: {}", id);
    }

    /**
     * Search orders (Admin only)
     */
    public PageResponse<OrderResponse> searchOrders(OrderSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate().atStartOfDay() : null;
        LocalDateTime endDate = request.getEndDate() != null ?
                request.getEndDate().atTime(23, 59, 59) : null;

        Page<Order> orders = orderRepository.searchOrders(
                request.getKeyword(),
                request.getStatus(),
                startDate,
                endDate,
                pageable
        );

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

    /**
     * Get order statistics
     */
    public OrderSummaryResponse getOrderSummary() {
        Map<String, Integer> ordersByStatus = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalOrders = 0;

        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            if (count > 0) {
                ordersByStatus.put(status.toString(), Math.toIntExact(count));
                totalOrders += count;
            }
        }

        // Get all orders to calculate total amount
        Page<Order> allOrders = orderRepository.findAll(PageRequest.of(0, Integer.MAX_VALUE));
        totalAmount = allOrders.getContent().stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderSummaryResponse.builder()
                .totalOrders(Math.toIntExact(totalOrders))
                .totalAmount(totalAmount)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    /**
     * Generate order number
     */
    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        String date = String.format("%04d%02d%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth());

        // Get next sequence number for today
        long count = orderRepository.findAll().stream()
                .filter(o -> o.getOrderNumber().contains(date))
                .count();

        return String.format("ORD-%s-%04d", date, count + 1);
    }

    /**
     * Calculate tax (10% of subtotal)
     */
    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(0.1));
    }

    /**
     * Calculate shipping based on address
     */
    private BigDecimal calculateShipping() {
        // Simple implementation: flat shipping cost
        return BigDecimal.valueOf(10); // $10 flat shipping
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Define valid transitions
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

    /**
     * Restore product stock when order is cancelled
     */
    private void restoreProductStock(Order order) {
        orderItemService.getOrderItems(order.getId()).forEach(item -> {
            try {
                // Restore stock by adding back quantity
                log.info("Restoring stock for product: {} quantity: {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Error restoring stock for product: {}", item.getProductId(), e);
            }
        });
    }

    /**
     * Get current user ID from security context
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * Get current user token
     */
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

    /**
     * Get current user email from security context.
     */
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

    /**
     * Publish Kafka event after successful transaction commit.
     */
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

    /**
     * Parse payment method from string
     */
    private com.ecommerce.orderservice.enums.PaymentMethod parsePaymentMethod(String method) {
        try {
            return com.ecommerce.orderservice.enums.PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment method: " + method);
        }
    }
}


