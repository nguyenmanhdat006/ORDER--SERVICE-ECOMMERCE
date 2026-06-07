package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.dto.request.OrderSearchRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> from(OrderSearchRequest request) {
        if (request == null) {
            return Specification.where(null);
        }

        LocalDateTime startDate = request.getStartDate() != null
                ? request.getStartDate().atStartOfDay()
                : null;
        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().atTime(23, 59, 59)
                : null;

        return Specification
                .where(matchesKeyword(request.getKeyword()))
                .and(hasStatus(request.getStatus()))
                .and(orderedAtFrom(startDate))
                .and(orderedAtTo(endDate));
    }

    private static Specification<Order> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("orderNumber")), pattern),
                cb.like(cb.lower(root.get("customerName")), pattern),
                cb.and(
                        cb.isNotNull(root.get("customerEmail")),
                        cb.like(cb.lower(root.get("customerEmail")), pattern)));
    }

    private static Specification<Order> hasStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Order> orderedAtFrom(LocalDateTime startDate) {
        if (startDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("orderedAt"), startDate);
    }

    private static Specification<Order> orderedAtTo(LocalDateTime endDate) {
        if (endDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("orderedAt"), endDate);
    }
}
