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
    private String trackingNumber;
}

