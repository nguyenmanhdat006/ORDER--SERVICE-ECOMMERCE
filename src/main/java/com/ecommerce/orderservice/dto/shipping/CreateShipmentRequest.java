package com.ecommerce.orderservice.dto.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShipmentRequest {
    private String orderId;
    private String orderNumber;
    private String recipientName;
    private String phone;
    private String address;
    private BigDecimal shippingFee;
    private BigDecimal codAmount;
    private Integer estimatedDays;
    private String note;
}

