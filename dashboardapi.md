# BACKEND API CONTRACTS - FOR DASHBOARD SERVICE

**API contracts mà các services cần implement để support Dashboard Service**

---

## 🎯 OVERVIEW

Dashboard Service cần call các endpoints sau từ:
1. **Order Service** - 8 endpoints
2. **User Service** - 2 endpoints
3. **Product Service** - 1 endpoint

---

## 📦 ORDER SERVICE - STATS ENDPOINTS

**Base URL:** `http://localhost:8084`

---

### **1. Count Pending Orders**

```
GET /api/orders/stats/pending-count
```

**Description:** Đếm số đơn hàng đang pending

**Response:** `200 OK`
```json
2040
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/pending-count")
public ResponseEntity<Long> countPendingOrders() {
    Long count = orderRepository.countByStatus(OrderStatus.PENDING);
    return ResponseEntity.ok(count);
}
```

**Query:**
```java
// OrderRepository
Long countByStatus(OrderStatus status);
```

---

### **2. Count Pending Orders Yesterday**

```
GET /api/orders/stats/pending-count-yesterday
```

**Description:** Đếm số đơn pending từ ngày hôm qua

**Response:** `200 OK`
```json
2004
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/pending-count-yesterday")
public ResponseEntity<Long> countPendingOrdersYesterday() {
    LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay();
    LocalDateTime today = LocalDate.now().atStartOfDay();
    
    Long count = orderRepository.countByStatusAndCreatedAtBetween(
        OrderStatus.PENDING, yesterday, today
    );
    return ResponseEntity.ok(count);
}
```

**Query:**
```java
// OrderRepository
Long countByStatusAndCreatedAtBetween(
    OrderStatus status, 
    LocalDateTime start, 
    LocalDateTime end
);
```

---

### **3. Get Total Sales**

```
GET /api/orders/stats/total-sales
```

**Description:** Tổng doanh thu từ các đơn CONFIRMED + DELIVERED

**Response:** `200 OK`
```json
89000000
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/total-sales")
public ResponseEntity<BigDecimal> getTotalSales() {
    List<OrderStatus> statuses = List.of(
        OrderStatus.CONFIRMED, 
        OrderStatus.DELIVERED
    );
    BigDecimal total = orderRepository.sumTotalByStatusIn(statuses);
    return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
}
```

**Query:**
```java
// OrderRepository
@Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN :statuses")
BigDecimal sumTotalByStatusIn(@Param("statuses") List<OrderStatus> statuses);
```

---

### **4. Get Total Sales Yesterday**

```
GET /api/orders/stats/total-sales-yesterday
```

**Description:** Tổng doanh thu từ ngày hôm qua

**Response:** `200 OK`
```json
85150000
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/total-sales-yesterday")
public ResponseEntity<BigDecimal> getTotalSalesYesterday() {
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
```

**Query:**
```java
// OrderRepository
@Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
       "WHERE o.status IN :statuses " +
       "AND o.createdAt >= :start AND o.createdAt < :end")
BigDecimal sumTotalByStatusInAndCreatedAtBetween(
    @Param("statuses") List<OrderStatus> statuses,
    @Param("start") LocalDateTime start,
    @Param("end") LocalDateTime end
);
```

---

### **5. Count Total Orders**

```
GET /api/orders/stats/total-count
```

**Description:** Tổng số đơn hàng (CONFIRMED + DELIVERED)

**Response:** `200 OK`
```json
10293
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/total-count")
public ResponseEntity<Long> countTotalOrders() {
    List<OrderStatus> statuses = List.of(
        OrderStatus.CONFIRMED, 
        OrderStatus.DELIVERED
    );
    Long count = orderRepository.countByStatusIn(statuses);
    return ResponseEntity.ok(count);
}
```

**Query:**
```java
// OrderRepository
Long countByStatusIn(List<OrderStatus> statuses);
```

---

### **6. Count Orders Last Week**

```
GET /api/orders/stats/count-last-week
```

**Description:** Số đơn hàng trong 7 ngày qua

**Response:** `200 OK`
```json
10160
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/count-last-week")
public ResponseEntity<Long> countOrdersLastWeek() {
    LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
    Long count = orderRepository.countByCreatedAtAfter(lastWeek);
    return ResponseEntity.ok(count);
}
```

**Query:**
```java
// OrderRepository
Long countByCreatedAtAfter(LocalDateTime date);
```

---

### **7. Get Sales By Date**

```
GET /api/orders/stats/sales-by-date?year=2024&month=10
```

**Description:** Doanh thu theo từng ngày trong tháng

**Query Parameters:**
- `year` (required) - Năm (VD: 2024)
- `month` (required) - Tháng (1-12)

**Response:** `200 OK`
```json
[
  {
    "date": "2024-10-01",
    "label": "1",
    "sales": 45000000,
    "orderCount": 15
  },
  {
    "date": "2024-10-02",
    "label": "2",
    "sales": 52000000,
    "orderCount": 18
  },
  {
    "date": "2024-10-03",
    "label": "3",
    "sales": 48500000,
    "orderCount": 16
  }
]
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/sales-by-date")
public ResponseEntity<List<SalesDataPoint>> getSalesByDate(
    @RequestParam int year,
    @RequestParam int month
) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
    
    List<Object[]> results = orderRepository.getSalesByDateBetween(
        startDate, endDate
    );
    
    List<SalesDataPoint> dataPoints = results.stream()
        .map(row -> new SalesDataPoint(
            ((java.sql.Date) row[0]).toLocalDate().toString(),
            String.valueOf(((java.sql.Date) row[0]).toLocalDate().getDayOfMonth()),
            (BigDecimal) row[1],
            ((Long) row[2]).intValue()
        ))
        .toList();
    
    return ResponseEntity.ok(dataPoints);
}
```

**Query:**
```java
// OrderRepository
@Query("SELECT DATE(o.createdAt) as date, " +
       "COALESCE(SUM(o.total), 0) as sales, " +
       "COUNT(o) as orderCount " +
       "FROM Order o " +
       "WHERE o.status IN ('CONFIRMED', 'DELIVERED') " +
       "AND o.createdAt >= :start AND o.createdAt <= :end " +
       "GROUP BY DATE(o.createdAt) " +
       "ORDER BY DATE(o.createdAt)")
List<Object[]> getSalesByDateBetween(
    @Param("start") LocalDateTime start,
    @Param("end") LocalDateTime end
);
```

**DTO:**
```java
public record SalesDataPoint(
    String date,
    String label,
    BigDecimal sales,
    Integer orderCount
) {}
```

---

### **8. Get Top Products**

```
GET /api/orders/stats/top-products?limit=10
```

**Description:** Top sản phẩm bán chạy nhất

**Query Parameters:**
- `limit` (optional, default: 10) - Số lượng sản phẩm

**Response:** `200 OK`
```json
[
  {
    "productId": "prod-123",
    "productName": "iPhone 15 Pro Max",
    "size": "256GB",
    "imageUrl": "https://example.com/iphone.jpg",
    "price": 29990000,
    "stock": 150,
    "category": "Electronics",
    "totalSold": 245,
    "totalRevenue": 7347550000
  },
  {
    "productId": "prod-456",
    "productName": "AirPods Pro",
    "size": null,
    "imageUrl": "https://example.com/airpods.jpg",
    "price": 5990000,
    "stock": 320,
    "category": "Electronics",
    "totalSold": 189,
    "totalRevenue": 1132110000
  }
]
```

**Implementation:**
```java
@GetMapping("/api/orders/stats/top-products")
public ResponseEntity<List<TopProduct>> getTopProducts(
    @RequestParam(defaultValue = "10") int limit
) {
    List<Object[]> results = orderRepository.getTopSellingProducts();
    
    List<TopProduct> products = results.stream()
        .limit(limit)
        .map(row -> new TopProduct(
            (String) row[0],      // productId
            (String) row[1],      // productName
            (String) row[2],      // size
            (String) row[3],      // imageUrl
            (BigDecimal) row[4],  // price
            (Integer) row[5],     // stock
            (String) row[6],      // category
            ((Long) row[7]).intValue(),     // totalSold
            (BigDecimal) row[8]   // totalRevenue
        ))
        .toList();
    
    return ResponseEntity.ok(products);
}
```

**Query:**
```java
// OrderRepository
@Query("SELECT oi.productId, oi.productName, oi.size, oi.imageUrl, " +
       "oi.price, oi.stock, oi.category, " +
       "SUM(oi.quantity) as totalSold, " +
       "SUM(oi.price * oi.quantity) as totalRevenue " +
       "FROM Order o JOIN o.items oi " +
       "WHERE o.status IN ('CONFIRMED', 'DELIVERED') " +
       "GROUP BY oi.productId, oi.productName, oi.size, oi.imageUrl, " +
       "oi.price, oi.stock, oi.category " +
       "ORDER BY totalSold DESC")
List<Object[]> getTopSellingProducts();
```

**DTO:**
```java
public record TopProduct(
    String productId,
    String productName,
    String size,
    String imageUrl,
    BigDecimal price,
    Integer stock,
    String category,
    Integer totalSold,
    BigDecimal totalRevenue
) {}
```

---

## 👤 USER SERVICE - STATS ENDPOINTS

**Base URL:** `http://localhost:8081`

---

### **1. Count Total Users**

```
GET /api/users/stats/total-count
```

**Description:** Tổng số users

**Response:** `200 OK`
```json
40689
```

**Implementation:**
```java
@GetMapping("/api/users/stats/total-count")
public ResponseEntity<Long> countTotalUsers() {
    Long count = userRepository.count();
    return ResponseEntity.ok(count);
}
```

---

### **2. Count Users Yesterday**

```
GET /api/users/stats/count-yesterday
```

**Description:** Số users đăng ký từ ngày hôm qua

**Response:** `200 OK`
```json
37512
```

**Implementation:**
```java
@GetMapping("/api/users/stats/count-yesterday")
public ResponseEntity<Long> countUsersYesterday() {
    LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay();
    LocalDateTime today = LocalDate.now().atStartOfDay();
    
    Long count = userRepository.countByCreatedAtBetween(yesterday, today);
    return ResponseEntity.ok(count);
}
```

**Query:**
```java
// UserRepository
Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
```

---

## 📦 PRODUCT SERVICE - ENDPOINTS

**Base URL:** `http://localhost:8082`

---

### **1. Get Product Details**

```
GET /api/products/{productId}
```

**Description:** Lấy chi tiết sản phẩm (optional - nếu Order Service không có đủ info)

**Path Parameters:**
- `productId` - ID của sản phẩm

**Response:** `200 OK`
```json
{
  "id": "prod-123",
  "name": "iPhone 15 Pro Max",
  "price": 29990000,
  "stock": 150,
  "category": "Electronics",
  "imageUrl": "https://example.com/iphone.jpg",
  "description": "...",
  "variants": [...]
}
```

**Implementation:**
```java
@GetMapping("/api/products/{productId}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));
    
    return ResponseEntity.ok(productMapper.toResponse(product));
}
```

---

## 🔧 IMPLEMENTATION SUMMARY

### **Order Service - New Controller:**

```java
package com.ecommerce.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders/stats")
@RequiredArgsConstructor
@Slf4j
public class OrderStatsController {
    
    private final OrderRepository orderRepository;
    
    @GetMapping("/pending-count")
    public ResponseEntity<Long> countPendingOrders() {
        // Implementation above
    }
    
    @GetMapping("/pending-count-yesterday")
    public ResponseEntity<Long> countPendingOrdersYesterday() {
        // Implementation above
    }
    
    @GetMapping("/total-sales")
    public ResponseEntity<BigDecimal> getTotalSales() {
        // Implementation above
    }
    
    @GetMapping("/total-sales-yesterday")
    public ResponseEntity<BigDecimal> getTotalSalesYesterday() {
        // Implementation above
    }
    
    @GetMapping("/total-count")
    public ResponseEntity<Long> countTotalOrders() {
        // Implementation above
    }
    
    @GetMapping("/count-last-week")
    public ResponseEntity<Long> countOrdersLastWeek() {
        // Implementation above
    }
    
    @GetMapping("/sales-by-date")
    public ResponseEntity<List<SalesDataPoint>> getSalesByDate(
        @RequestParam int year,
        @RequestParam int month
    ) {
        // Implementation above
    }
    
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProduct>> getTopProducts(
        @RequestParam(defaultValue = "10") int limit
    ) {
        // Implementation above
    }
}
```

---

### **User Service - New Controller:**

```java
package com.ecommerce.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api/users/stats")
@RequiredArgsConstructor
public class UserStatsController {
    
    private final UserRepository userRepository;
    
    @GetMapping("/total-count")
    public ResponseEntity<Long> countTotalUsers() {
        // Implementation above
    }
    
    @GetMapping("/count-yesterday")
    public ResponseEntity<Long> countUsersYesterday() {
        // Implementation above
    }
}
```

---

## ✅ CHECKLIST

### **Order Service:**
```
[ ] Create OrderStatsController
[ ] Add 8 query methods to OrderRepository
[ ] Create SalesDataPoint DTO
[ ] Create TopProduct DTO
[ ] Test all endpoints
[ ] Update OrderItem entity to include size, imageUrl, stock, category
```

### **User Service:**
```
[ ] Create UserStatsController
[ ] Add countByCreatedAtBetween query
[ ] Test both endpoints
```

### **Product Service:**
```
[ ] GET /api/products/{id} already exists (no changes needed)
```

---

## 🧪 TESTING

```bash
# Order Service Stats
curl http://localhost:8084/api/orders/stats/pending-count
curl http://localhost:8084/api/orders/stats/total-sales
curl http://localhost:8084/api/orders/stats/sales-by-date?year=2024&month=10
curl http://localhost:8084/api/orders/stats/top-products?limit=5

# User Service Stats
curl http://localhost:8081/api/users/stats/total-count
curl http://localhost:8081/api/users/stats/count-yesterday

# Product Service
curl http://localhost:8082/api/products/prod-123
```

---

## 🔑 KEY POINTS

1. **Simple responses:** Trả về primitive types (Long, BigDecimal) hoặc simple DTOs
2. **No complex logic:** Chỉ query database và trả về
3. **Fast queries:** Add indexes nếu cần
4. **No auth required:** Dashboard Service sẽ handle auth
5. **Cacheable:** Dashboard Service sẽ cache responses

---

**BACKEND API CONTRACTS COMPLETE! 🎯**

Copy contracts này để implement ở Order Service + User Service.