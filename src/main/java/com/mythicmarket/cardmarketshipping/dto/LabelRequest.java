package com.mythicmarket.cardmarketshipping.dto;

public record LabelRequest(
        String buyerName,
        String street,
        String postalCode,
        String city,
        String countryName,
        String orderId,
        int weightGrams,
        String serviceType
) {
}
