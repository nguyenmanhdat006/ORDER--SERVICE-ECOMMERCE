package com.ecommerce.orderservice.dto.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShipmentResponse {
    private Long shipmentId;
    private String shipmentNumber;
    private String orderId;
    private String orderNumber;
    private String status;
    private java.math.BigDecimal shippingFee;
    private java.math.BigDecimal codAmount;
    private java.time.LocalDateTime estimatedDelivery;
    private java.time.LocalDateTime createdAt;
}

