package com.ecommerce.orderservice.dto.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateFeeRequest {
    private Integer toDistrictId;
    private String toWardCode;
    private Integer weight;
}

