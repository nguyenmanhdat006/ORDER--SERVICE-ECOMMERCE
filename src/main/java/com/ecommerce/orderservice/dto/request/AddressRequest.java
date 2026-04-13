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
@Schema(description = "Address information for shipping or billing")
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Schema(description = "Full name of recipient", example = "Nguyen Van A", required = true)
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Schema(description = "Contact phone number", example = "0901234567", required = true)
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Schema(description = "Primary address line", example = "123 Nguyen Hue Street", required = true)
    private String addressLine1;

    @Schema(description = "Secondary address line", example = "Apartment 5B")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Schema(description = "City name", example = "Ho Chi Minh City", required = true)
    private String city;

    @NotBlank(message = "State is required")
    @Schema(description = "State/Province", example = "HCM", required = true)
    private String state;

    @NotBlank(message = "Zip code is required")
    @Schema(description = "Postal/ZIP code", example = "700000", required = true)
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Schema(description = "Country name", example = "Vietnam", required = true)
    private String country;
}

