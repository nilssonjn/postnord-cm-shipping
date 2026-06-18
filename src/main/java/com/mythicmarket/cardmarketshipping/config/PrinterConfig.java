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
    private String name;

    public boolean isLocalMode() {
        return name != null && !name.isBlank();
    }

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        if (isLocalMode()) {
            log.info("Printer mode: local (javax.print) — printer name: '{}'", name);
        } else {
            if (host == null || host.isBlank()) {
                log.warn("printer.enabled=true but neither printer.name nor printer.host is set — printing will fail");
            }
            if (port <= 0) {
                log.warn("printer.enabled=true but printer.port is invalid '{}' — printing will fail", port);
            }
        }
    }

}
