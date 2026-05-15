package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.client.ShippingServiceClient;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentRequest;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.enums.PaymentMethod;
import com.ecommerce.orderservice.enums.PaymentStatus;
import com.ecommerce.orderservice.event.OrderEventProducer;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessService {

    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final OrderStatusService orderStatusService;
    private final CartServiceClient cartServiceClient;
    private final ShippingServiceClient shippingServiceClient;
    private final OrderEventProducer orderEventProducer;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executePostOrderOperations(UUID orderId, String token) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Skipping post-order operations because order was not found: {}", orderId);
            return;
        }

        log.info("Executing post-order operations for order: {}", order.getOrderNumber());

        try {
            if (token != null && !token.isBlank()) {
                cartServiceClient.clearCart(order.getUserId(), token);
            } else {
                log.warn("Skipping cart clear for order {} because auth token is missing", order.getOrderNumber());
            }

            // MIGRATION: COD confirmation and shipment creation moved to synchronous OrderService.createOrder()
            // for better error handling and immediate feedback to client
            /*
            if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY && order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setConfirmedAt(LocalDateTime.now());
                orderRepository.save(order);
                orderStatusService.addStatusHistory(order, OrderStatus.CONFIRMED, "Order confirmed for COD", "system");
                orderEventProducer.publishOrderConfirmed(order);
            }

            if (order.getStatus() == OrderStatus.CONFIRMED) {
                CreateShipmentRequest shipmentRequest = CreateShipmentRequest.builder()
                        .orderId(order.getId().toString())
                        .orderNumber(order.getOrderNumber())
                        .toName(order.getCustomerName())
                        .toPhone(order.getCustomerPhone())
                        .toAddress(extractAddress(order.getShippingAddress()))
                        .toDistrictId(extractDistrictId(order.getShippingAddress()))
                        .toWardCode(extractWardCode(order.getShippingAddress()))
                        .weight(calculateOrderWeight(order.getId()))
                        .codAmount(order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY ? order.getTotal() : BigDecimal.ZERO)
                        .note("Order " + order.getOrderNumber())
                        .build();

                CreateShipmentResponse shipmentResponse = shippingServiceClient.createShipment(shipmentRequest);
                order.setShipmentId(shipmentResponse.getShipmentNumber());
                order.setTrackingNumber(shipmentResponse.getTrackingNumber());
                orderRepository.save(order);
            }
            */
            
            log.debug("Post-order operationpublics completed (most operations moved to synchronous flow)");
        } catch (Exception e) {
            log.error("Error in post-order operations for order: {}", order.getOrderNumber(), e);
        }
    }

    public Integer calculateOrderWeight(UUID orderId) {
        int quantity = orderItemService.getOrderItems(orderId).stream()
                .map(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .reduce(0, Integer::sum);
        return Math.max(quantity, 1) * 1000;
    }
    
    // Public methods for synchronous shipment creation (used by OrderService for COD orders)
    public Integer extractDistrictId(JsonNode shippingAddress) {
        if (shippingAddress == null || shippingAddress.isNull()) {
            return 0;
        }
        if (shippingAddress.has("districtId") && shippingAddress.get("districtId").canConvertToInt()) {
            return shippingAddress.get("districtId").asInt();
        }
        if (shippingAddress.has("zipCode")) {
            try {
                return Integer.parseInt(shippingAddress.get("zipCode").asText("0"));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public String extractWardCode(JsonNode shippingAddress) {
        if (shippingAddress == null || shippingAddress.isNull()) {
            return "";
        }
        if (shippingAddress.has("wardCode")) {
            return shippingAddress.get("wardCode").asText("");
        }
        return shippingAddress.has("state") ? shippingAddress.get("state").asText("") : "";
    }

    public String extractAddress(JsonNode shippingAddress) {
        if (shippingAddress == null || shippingAddress.isNull()) {
            return "";
        }

        String line1 = shippingAddress.has("addressLine1") ? shippingAddress.get("addressLine1").asText("") : "";
        String line2 = shippingAddress.has("addressLine2") ? shippingAddress.get("addressLine2").asText("") : "";
        String city = shippingAddress.has("city") ? shippingAddress.get("city").asText("") : "";

        return String.join(", ", java.util.List.of(line1, line2, city).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }
}

