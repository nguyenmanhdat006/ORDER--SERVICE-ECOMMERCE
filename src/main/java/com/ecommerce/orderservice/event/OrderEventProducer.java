package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CONFIRMED_TOPIC = "order.confirmed";
    private static final String ORDER_SHIPPED_TOPIC = "order.shipped";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(Order order, List<OrderItem> items) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .timestamp(LocalDateTime.now())
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getCustomerEmail())
                .items(mapOrderItems(items))
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shipping(order.getShipping())
                .tax(order.getTax())
                .total(order.getTotal())
                .shippingAddress(mapAddress(order.getShippingAddress()))
                .billingAddress(mapAddress(order.getBillingAddress()))
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .build();

        publishEvent(ORDER_CREATED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderStatusChanged(Order order, OrderStatus status, String reason) {
        if (status == OrderStatus.CONFIRMED) {
            publishOrderConfirmed(order);
        } else if (status == OrderStatus.SHIPPED) {
            publishOrderShipped(order);
        } else if (status == OrderStatus.CANCELLED) {
            publishOrderCancelled(order, reason);
        }
    }

    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CONFIRMED")
                .timestamp(LocalDateTime.now())
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getCustomerEmail())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .build();

        publishEvent(ORDER_CONFIRMED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderShipped(Order order) {
        OrderShippedEvent event = OrderShippedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_SHIPPED")
                .timestamp(LocalDateTime.now())
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getCustomerEmail())
                .total(order.getTotal())
                .build();

        publishEvent(ORDER_SHIPPED_TOPIC, order.getId().toString(), event);
    }

    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CANCELLED")
                .timestamp(LocalDateTime.now())
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getCustomerEmail())
                .reason(reason)
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .build();

        publishEvent(ORDER_CANCELLED_TOPIC, order.getId().toString(), event);
    }

    private void publishEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published Kafka event: topic={}, key={}, partition={}, offset={}",
                            topic,
                            key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish Kafka event: topic={}, key={}", topic, key, ex);
                }
            });
        } catch (Exception ex) {
            log.error("Unexpected error while publishing Kafka event: topic={}, key={}", topic, key, ex);
        }
    }

    private List<OrderCreatedEvent.OrderItemDto> mapOrderItems(List<OrderItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                        .productId(item.getProductId())
                        .productVariantId(item.getProductVariantId())
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());
    }

    private OrderCreatedEvent.AddressDto mapAddress(JsonNode addressJson) {
        if (addressJson == null || addressJson.isNull() || addressJson.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.treeToValue(addressJson, OrderCreatedEvent.AddressDto.class);
        } catch (Exception ex) {
            log.warn("Failed to map address JSON to Kafka event payload", ex);
            return null;
        }
    }
}


