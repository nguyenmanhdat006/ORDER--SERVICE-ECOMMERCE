package com.ecommerce.orderservice.dto.request;

import com.ecommerce.orderservice.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;

@Getter
@Setter
@ParameterObject
public class OrderFilterParams {

    private String keyword;

    private OrderStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    private String sortBy = "orderedAt";

    private String sortDirection = "desc";

    private int page = 0;

    private int size = 20;
}
