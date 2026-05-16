package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.response.AddressResponse;
import com.ecommerce.orderservice.dto.response.OrderItemResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatusHistory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Mapper
 */
@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "status", expression = "java(order.getStatus().toString())")
    @Mapping(target = "paymentStatus", expression = "java(mapPaymentStatus(order.getPaymentStatus()))")
    @Mapping(target = "paymentMethod", expression = "java(mapPaymentMethod(order.getPaymentMethod()))")
    @Mapping(target = "items", expression = "java(mapOrderItems(order.getItems()))")
    @Mapping(target = "shippingAddress", expression = "java(parseAddress(order.getShippingAddress()))")
    public abstract OrderResponse toResponse(Order order);

    @Mapping(target = "productVariantId", source = "productVariantId")
    public abstract OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "status", expression = "java(history.getStatus().toString())")
    public abstract OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history);

    public List<OrderItemResponse> mapOrderItems(List<OrderItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    public List<OrderStatusHistoryResponse> mapStatusHistory(List<OrderStatusHistory> history) {
        if (history == null) {
            return null;
        }
        return history.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    protected AddressResponse parseAddress(JsonNode addressJson) {
        if (addressJson == null || addressJson.isNull() || addressJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(addressJson, AddressResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    protected String mapPaymentStatus(com.ecommerce.orderservice.enums.PaymentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "PENDING";
            case PAID -> "SUCCESS";
            case FAILED -> "FAILED";
            case REFUNDED -> "REFUNDED";
        };
    }

    protected String mapPaymentMethod(com.ecommerce.orderservice.enums.PaymentMethod method) {
        if (method == null) {
            return null;
        }
        return switch (method) {
            case CASH_ON_DELIVERY -> "COD";
            case VNPAY -> "VNPAY";
            default -> method.name();
        };
    }
}

