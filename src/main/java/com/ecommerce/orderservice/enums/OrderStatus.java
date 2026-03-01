package com.ecommerce.orderservice.enums;

/**
 * Order Status Enumeration
 */
public enum OrderStatus {
    PENDING,           // Order created, awaiting payment
    CONFIRMED,         // Payment confirmed
    PROCESSING,        // Being prepared
    SHIPPED,          // On delivery
    DELIVERED,        // Completed
    CANCELLED,        // Cancelled by user/admin
    REFUNDED         // Payment refunded
}

