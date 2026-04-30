package com.mythicmarket.cardmarketshipping.dto;

import lombok.Data;

@Data
public class LabelRequest {

    private String buyerName;
    private String street;
    private String postalCode;
    private String city;
    private String countryName;
    private String orderId;
    private int weightGrams;
    private String serviceType;
}
