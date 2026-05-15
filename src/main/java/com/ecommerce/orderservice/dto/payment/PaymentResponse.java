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
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String paymentUrl;
}

