package com.mythicmarket.cardmarketshipping.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Map;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "postnord")
@Data
public class PostNordConfig {

    private String apiKey;
    private String issuerCode;
    private String customerNumber;
    private Sender sender;
    private boolean testMode;
    private String defaultPackageType;
    private String url;
    private String urlParams;
    private Map<String, String> aliases;
    private Map<String, ServiceEntry> serviceCodes;
    private Application application;

    @PostConstruct
    public void validateEnv() {
        if (!new File(".env").exists()) {
            log.warn(".env file not found in working directory '{}' — configuration may be incomplete", new File(".").getAbsolutePath());
        }

        checkProperty(apiKey, "postnord.api-key");
        checkProperty(issuerCode, "postnord.issuer-code");
        checkProperty(customerNumber, "postnord.customer-number");
        checkProperty(url, "postnord.url");

        if (sender == null) {
            log.warn("Missing config: postnord.sender");
        } else {
            checkProperty(sender.getName(), "postnord.sender.name");
            checkProperty(sender.getStreet(), "postnord.sender.street");
            checkProperty(sender.getZipCode(), "postnord.sender.zip-code");
            checkProperty(sender.getCity(), "postnord.sender.city");
        }
    }

    private void checkProperty(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            log.warn("Missing or blank config: {}", propertyName);
        }
    }

    @Data
    public static class Application {
        private Integer applicationId;
        private String name;
        private String version;
    }

    @Data
    public static class ServiceEntry {
        private String code;
        private String addon;
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
