package com.ecommerce.orderservice.dto.response;

import java.math.BigDecimal;

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
