package com.mythicmarket.cardmarketshipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LabelRequest(
        @NotBlank String buyerName,
        String buyerPhone,
        String buyerEmail,
        @NotBlank String street,
        String optionalStreet,
        @NotBlank String postalCode,
        @NotBlank String city,
        @NotBlank String countryName,
        @NotBlank String orderId,
        @Positive int weightGrams,
        @NotBlank String serviceType
) {
}
