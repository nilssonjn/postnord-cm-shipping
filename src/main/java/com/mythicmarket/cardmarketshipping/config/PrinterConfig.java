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

    @PostConstruct
    public void validate() {
        if (enabled && (host == null || host.isEmpty())) {
            log.warn("printer.enabled=true but printer.host is not set, '{}' - printing will fail", host);
        }
    }

}
