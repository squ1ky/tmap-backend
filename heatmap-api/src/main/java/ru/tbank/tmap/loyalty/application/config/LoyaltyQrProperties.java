package ru.tbank.tmap.loyalty.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qr")
public record LoyaltyQrProperties(
        int ttlSeconds
) {
}
