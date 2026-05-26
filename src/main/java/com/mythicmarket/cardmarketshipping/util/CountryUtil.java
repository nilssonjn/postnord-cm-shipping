package com.mythicmarket.cardmarketshipping.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class CountryUtil {

    public static String toIsoCode(String countryName) {
        for (String iso : Locale.getISOCountries()) {
            if (Locale.of("", iso).getDisplayCountry(Locale.ENGLISH)
                    .equalsIgnoreCase(countryName)) {
                return iso;
            }
        }
        log.error("Unknown country name received from Cardmarket: {}", countryName);
        throw new IllegalArgumentException("Unknown country: " + countryName);
    }
}
