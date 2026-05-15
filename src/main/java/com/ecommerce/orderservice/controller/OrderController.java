package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.request.OrderSearchRequest;
import com.ecommerce.orderservice.dto.request.PaymentConfirmRequest;
import com.ecommerce.orderservice.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.orderservice.dto.response.ApiResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderSummaryResponse;
import com.ecommerce.orderservice.dto.response.PageResponse;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders in e-commerce system")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(
            summary = "Create order from cart",
            description = "Creates a new order from the current user's cart. Validates stock, calculates totals, and clears cart after successful order creation."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Cart empty or invalid data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating order with payment method: {}", request.getPaymentMethod());
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }

    @GetMapping
    @Operation(
            summary = "Get my orders",
            description = "Retrieves all orders for the authenticated user with pagination support."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully"
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching orders - page: {}, size: {}", page, size);
        PageResponse<OrderResponse> orders = orderService.getMyOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves detailed information about a specific order. User can only access their own orders."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Order belongs to another user"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        log.info("Fetching order: {}", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(
            summary = "Get order by order number",
            description = "Find order using the unique order number (e.g., ORD-20260228-0001)"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @Parameter(description = "Order number", example = "ORD-20260228-0001")
            @PathVariable String orderNumber) {
        log.info("Fetching order by number: {}", orderNumber);
        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update order status (Admin)",
            description = "Updates the status of an order. Only admins can perform this action. Status transitions are validated."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order status for order: {} to status: {}", id, request.getStatus());
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel order",
            description = "Cancels an order. Only orders in PENDING or CONFIRMED status can be cancelled. Stock will be restored."
    )
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {
        log.info("Cancelling order: {} with reason: {}", id, request.getReason());
        orderService.cancelOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Search orders (Admin)",
            description = "Advanced search for orders with filters: keyword, status, date range. Supports pagination."
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> searchOrders(
            @Parameter(description = "Search keyword (order number, customer name/email)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Order status filter")
            @RequestParam(required = false) String status,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Searching orders - keyword: {}, status: {}, page: {}, size: {}",
                keyword, status, page, size);

        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .keyword(keyword)
                .status(status != null ? com.ecommerce.orderservice.enums.OrderStatus.valueOf(status.toUpperCase()) : null)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();

        PageResponse<OrderResponse> orders = orderService.searchOrders(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @PostMapping("/{id}/payment-confirmed")
    @Operation(
            summary = "Payment callback",
            description = "Handles payment service callback and confirms order when payment is successful."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody PaymentConfirmRequest request) {
        log.info("Payment callback received for order: {}", id);
        OrderResponse order = orderService.confirmPayment(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Payment callback processed"));
    }

    @PostMapping("/{id}/shipment-confirmed")
    @Operation(
            summary = "Shipment created callback",
            description = "Handles shipping service callback after shipment is created. Migration from Kafka events."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> confirmShipment(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @RequestParam(required = false) String shipmentId,
            @RequestParam(required = false) String trackingNumber) {
        log.info("Shipment callback received for order: {}, shipmentId: {}, trackingNumber: {}", 
                id, shipmentId, trackingNumber);
        OrderResponse order = orderService.confirmShipment(id, shipmentId, trackingNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Shipment confirmed"));
    }

    @PostMapping("/{id}/delivery-completed")
    @Operation(
            summary = "Mark order as delivered",
            description = "Marks order as DELIVERED and updates payment status to SUCCESS for COD orders. Migration from Kafka events."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> markDeliveryCompleted(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        log.info("Marking delivery completed for order: {}", id);
        OrderResponse order = orderService.markDeliveryCompleted(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order marked as delivered"));
    }


    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get order statistics (Admin)",
            description = "Retrieves order statistics including total orders, total amount, and breakdown by status."
    )
    public ResponseEntity<ApiResponse<OrderSummaryResponse>> getOrderSummary() {
        log.info("Fetching order summary");
        OrderSummaryResponse summary = orderService.getOrderSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Summary retrieved successfully"));
    }
//
//    @PutMapping("/{id}/shipping")
//    public ResponseEntity<?> updateShipping(
//            @PathVariable UUID id,
//            @RequestBody UpdateShippingRequest request) {
//
//        orderService.updateShipping(id, request);
//        return ResponseEntity.ok().build();
//    }
}

