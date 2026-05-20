package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE " +
           "(:keyword IS NULL OR o.orderNumber LIKE CONCAT('%', :keyword, '%') " +
           "OR o.customerName LIKE CONCAT('%', :keyword, '%') " +
           "OR o.customerEmail LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:startDate IS NULL OR o.orderedAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderedAt <= :endDate)")
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByStatus(OrderStatus status);

    Long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN :statuses")
    java.math.BigDecimal sumTotalByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
           "WHERE o.status IN :statuses " +
           "AND o.createdAt >= :start AND o.createdAt < :end")
    java.math.BigDecimal sumTotalByStatusInAndCreatedAtBetween(
        @Param("statuses") List<OrderStatus> statuses,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    Long countByStatusIn(List<OrderStatus> statuses);

    Long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT CAST(o.createdAt AS date) as date, " +
           "COALESCE(SUM(o.total), 0) as sales, " +
           "COUNT(o) as orderCount " +
           "FROM Order o " +
           "WHERE o.status IN ('CONFIRMED', 'DELIVERED') " +
           "AND o.createdAt >= :start AND o.createdAt <= :end " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date)")
    List<Object[]> getSalesByDateBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT oi.productId, oi.productName, oi.productImageUrl, " +
           "oi.price, " +
           "SUM(oi.quantity) as totalSold, " +
           "SUM(oi.price * oi.quantity) as totalRevenue " +
           "FROM Order o JOIN o.items oi " +
           "WHERE o.status IN ('CONFIRMED', 'DELIVERED') " +
           "GROUP BY oi.productId, oi.productName, oi.productImageUrl, oi.price " +
           "ORDER BY totalSold DESC")
    List<Object[]> getTopSellingProducts();
}

