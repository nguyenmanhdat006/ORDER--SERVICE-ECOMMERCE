package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatusHistory;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Status Service
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderMapper orderMapper;

    /**
     * Add status history record
     */
    public void addStatusHistory(Order order, OrderStatus newStatus, String notes, String changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .notes(notes)
                .changedBy(changedBy)
                .build();

        orderStatusHistoryRepository.save(history);
        log.info("Status history added for order: {} with status: {}", order.getId(), newStatus);
    }

    /**
     * Get order status history
     */
    public List<OrderStatusHistoryResponse> getOrderHistory(java.util.UUID orderId) {
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
        return history.stream()
                .map(orderMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }
}

