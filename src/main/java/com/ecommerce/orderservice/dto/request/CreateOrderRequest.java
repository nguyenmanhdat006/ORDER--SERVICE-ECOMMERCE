package com.ecommerce.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

    @Valid
    @NotEmpty(message = "Order items are required")
    private List<OrderItemRequest> items;

    @NotBlank(message = "Payment method is required")
    @Schema(description = "Payment method", example = "COD",
            allowableValues = {"COD", "VNPAY"})
    private String paymentMethod;

    @Valid
    @jakarta.validation.constraints.NotNull(message = "Shipping address is required")
    private AddressRequest shippingAddress;

    @Schema(description = "Additional notes for the order", example = "Please deliver before 5 PM")
    private String note;
}

