package com.ecommerce.orderservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shipping address information")
public class AddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Schema(description = "Recipient name", example = "Nguyen Van A", required = true)
    private String recipientName;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Contact phone number", example = "0901234567", required = true)
    private String phone;

    @NotBlank(message = "Address is required")
    @Schema(description = "Street address", example = "123 Le Loi, District 1", required = true)
    private String address;

    @NotBlank(message = "City is required")
    @Schema(description = "City", example = "Ho Chi Minh", required = true)
    private String city;

    @NotBlank(message = "Province is required")
    @Schema(description = "Province", example = "Ho Chi Minh", required = true)
    private String province;

    @NotBlank(message = "Zip code is required")
    @Schema(description = "Postal/ZIP code", example = "700000", required = true)
    private String zipCode;
}

