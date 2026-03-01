package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.response.AddressResponse;
import com.ecommerce.orderservice.dto.response.OrderItemResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatusHistory;
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
    @Mapping(target = "paymentStatus", expression = "java(order.getPaymentStatus().toString())")
    @Mapping(target = "paymentMethod", expression = "java(order.getPaymentMethod().toString())")
    @Mapping(target = "items", expression = "java(mapOrderItems(order.getItems()))")
    @Mapping(target = "shippingAddress", expression = "java(parseAddress(order.getShippingAddress()))")
    @Mapping(target = "billingAddress", expression = "java(parseAddress(order.getBillingAddress()))")
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

    protected AddressResponse parseAddress(String addressJson) {
        if (addressJson == null || addressJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(addressJson, AddressResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}

