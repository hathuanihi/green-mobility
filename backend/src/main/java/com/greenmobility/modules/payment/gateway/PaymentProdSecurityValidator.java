package com.greenmobility.modules.payment.gateway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class PaymentProdSecurityValidator implements CommandLineRunner {

    private final GatewayProperties properties;

    public PaymentProdSecurityValidator(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String tmn = properties.getVnpay().getTmnCode();
        String secret = properties.getVnpay().getHashSecret();
        String momoSecret = properties.getMomo().getSecretKey();

        if (isPlaceholder(tmn) || isPlaceholder(secret) || isPlaceholder(momoSecret)) {
            throw new IllegalStateException("FATAL: Production mode started with placeholder payment secrets! Refusing to start.");
        }
    }

    private boolean isPlaceholder(String val) {
        if (val == null || val.isBlank()) return true;
        String lower = val.toLowerCase();
        return lower.contains("demo") || lower.contains("sandbox") || lower.contains("placeholder") || lower.contains("replace");
    }
}

