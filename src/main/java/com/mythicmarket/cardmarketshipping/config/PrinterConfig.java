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

    private String host;
    private int port;
    private boolean enabled;
    private int connectTimeoutMs;

    @PostConstruct
    public void validate() {
        if (enabled && (host == null || host.isBlank())) {
            log.warn("printer.enabled=true but printer.host is not set — printing will fail");
        }
        if (enabled && port <= 0) {
            log.warn("printer.enabled=true but printer.port is invalid '{}' — printing will fail", port);
        }
    }

}
