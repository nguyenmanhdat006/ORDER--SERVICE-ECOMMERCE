package com.ecommerce.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new order from cart")
public class CreateOrderRequest {

    @NotBlank(message = "Payment method is required")
    @Schema(description = "Payment method", example = "COD",
            allowableValues = {"COD", "CASH_ON_DELIVERY", "VNPAY", "CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER", "MOMO"})
    private String paymentMethod;

    @Valid
    @jakarta.validation.constraints.NotNull(message = "Shipping address is required")
    private AddressRequest shippingAddress;

    @Valid
    @Schema(description = "Billing address (optional, defaults to shipping address)")
    private AddressRequest billingAddress;

    @Schema(description = "Additional notes for the order", example = "Please deliver before 5 PM")
    private String notes;
}

