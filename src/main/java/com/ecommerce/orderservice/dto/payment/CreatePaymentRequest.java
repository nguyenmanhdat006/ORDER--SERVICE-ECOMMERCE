package com.ecommerce.orderservice.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {
    private String orderId;
    private String orderNumber;
    private String userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String description;
}

