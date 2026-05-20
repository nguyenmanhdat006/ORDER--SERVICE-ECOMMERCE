package com.ecommerce.orderservice.dto.response;

import java.math.BigDecimal;

public record SalesDataPoint(
    String date,
    String label,
    BigDecimal sales,
    Integer orderCount
) {}
