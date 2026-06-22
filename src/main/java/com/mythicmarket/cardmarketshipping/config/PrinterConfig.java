package com.mythicmarket.cardmarketshipping.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "printer")
@Data
public class PrinterConfig {

    private boolean enabled;
    private String name;

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        if (name == null || name.isBlank()) {
            log.warn("printer.enabled=true but printer.name is not set — printing will fail");
        } else {
            log.info("Printer: '{}'", name);
        }
    }

}
