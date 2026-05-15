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
    private String toName;
    private String toPhone;
    private String toAddress;
    private Integer toDistrictId;
    private String toWardCode;
    private Integer weight;
    private BigDecimal codAmount;
    private String note;
}

