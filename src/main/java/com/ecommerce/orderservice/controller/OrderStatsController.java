package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.response.SalesDataPoint;
import com.ecommerce.orderservice.dto.response.TopProduct;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/orders/stats")
@RequiredArgsConstructor
@Slf4j
public class OrderStatsController {

    private final OrderRepository orderRepository;
    private final com.ecommerce.orderservice.client.ProductServiceClient productServiceClient;

    @GetMapping("/pending-count")
    public ResponseEntity<Long> countPendingOrders() {
        log.info("Counting pending orders");
        Long count = orderRepository.countByStatus(OrderStatus.PENDING);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/pending-count-yesterday")
    public ResponseEntity<Long> countPendingOrdersYesterday() {
        log.info("Counting pending orders from yesterday");
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        Long count = orderRepository.countByStatusAndCreatedAtBetween(
            OrderStatus.PENDING, yesterday, today
        );
        return ResponseEntity.ok(count);
    }

    @GetMapping("/total-sales")
    public ResponseEntity<BigDecimal> getTotalSales() {
        log.info("Getting total sales");
        List<OrderStatus> statuses = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED
        );
        BigDecimal total = orderRepository.sumTotalByStatusIn(statuses);
        return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
    }

    @GetMapping("/total-sales-yesterday")
    public ResponseEntity<BigDecimal> getTotalSalesYesterday() {
        log.info("Getting total sales from yesterday");
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        List<OrderStatus> statuses = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED
        );

        BigDecimal total = orderRepository.sumTotalByStatusInAndCreatedAtBetween(
            statuses, yesterday, today
        );
        return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
    }

    @GetMapping("/total-count")
    public ResponseEntity<Long> countTotalOrders() {
        log.info("Counting total orders (confirmed and delivered)");
        List<OrderStatus> statuses = List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED
        );
        Long count = orderRepository.countByStatusIn(statuses);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count-last-week")
    public ResponseEntity<Long> countOrdersLastWeek() {
        log.info("Counting orders in the last 7 days");
        LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
        Long count = orderRepository.countByCreatedAtAfter(lastWeek);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/sales-by-date")
    public ResponseEntity<List<SalesDataPoint>> getSalesByDate(
        @RequestParam int year,
        @RequestParam int month
    ) {
        log.info("Getting sales by date for year={}, month={}", year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> results = orderRepository.getSalesByDateBetween(
            startDate, endDate
        );

        List<SalesDataPoint> dataPoints = results.stream()
            .map(row -> {
                String dateStr = "";
                String label = "";
                if (row[0] != null) {
                    dateStr = row[0].toString();
                    try {
                        if (row[0] instanceof java.sql.Date sqlDate) {
                            label = String.valueOf(sqlDate.toLocalDate().getDayOfMonth());
                        } else if (row[0] instanceof java.time.LocalDate localDate) {
                            label = String.valueOf(localDate.getDayOfMonth());
                        } else {
                            java.time.LocalDate parsed = java.time.LocalDate.parse(dateStr);
                            label = String.valueOf(parsed.getDayOfMonth());
                        }
                    } catch (Exception e) {
                        label = dateStr;
                    }
                }
                BigDecimal sales = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                Integer count = row[2] != null ? ((Number) row[2]).intValue() : 0;

                return new SalesDataPoint(dateStr, label, sales, count);
            })
            .toList();

        return ResponseEntity.ok(dataPoints);
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProduct>> getTopProducts(
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting top products limit={}", limit);
        List<Object[]> results = orderRepository.getTopSellingProducts();

        List<TopProduct> products = results.stream()
            .limit(limit)
            .map(row -> {
                String productId = (String) row[0];
                String productName = (String) row[1];
                String productImageUrl = (String) row[2];
                BigDecimal price = (BigDecimal) row[3];
                Integer totalSold = row[4] != null ? ((Number) row[4]).intValue() : 0;
                BigDecimal totalRevenue = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;

                // Dynamically enrich details from Product Service
                String imageUrl = productImageUrl;
                Integer stock = 0;
                String category = "Electronics"; // Default mock
                String size = "Standard"; // Default mock

                try {
                    var product = productServiceClient.getProduct(productId);
                    if (product != null) {
                        if (product.imageUrl != null && !product.imageUrl.isBlank()) {
                            imageUrl = product.imageUrl;
                        }
                        if (product.stockQuantity != null) {
                            stock = product.stockQuantity;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch product details for {}, using fallback snapshot details", productId, e);
                }

                return new TopProduct(
                    productId,
                    productName,
                    size,
                    imageUrl,
                    price,
                    stock,
                    category,
                    totalSold,
                    totalRevenue
                );
            })
            .toList();

        return ResponseEntity.ok(products);
    }
}
