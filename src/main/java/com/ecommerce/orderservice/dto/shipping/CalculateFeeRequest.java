package com.ecommerce.orderservice.dto.shipping;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateFeeRequest {
    private String city;
    private String province;
    private Integer weight;
    private BigDecimal orderValue;
}

