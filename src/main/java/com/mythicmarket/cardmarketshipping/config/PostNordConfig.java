package com.mythicmarket.cardmarketshipping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "postnord")
@Data
public class PostNordConfig {

    private String apiKey;
    private String issuerCode;
    private String customerNumber;
    private ServiceCodes serviceCodes;
    private Sender sender;
    private boolean testMode;
    private String defaultPackageType;
    private String url;
    private String paperSize;

    @Data
    public static class ServiceCodes {
        private String domestic;
        private String international;
    }

    @Data
    public static class Sender {
        private String name;
        private String street;
        private String zipCode;
        private String city;
        private String countryCode;
    }

}
