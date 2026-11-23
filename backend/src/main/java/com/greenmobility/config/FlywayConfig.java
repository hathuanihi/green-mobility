package com.greenmobility.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(Environment env) {
        return flyway -> {
            boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
            if (!isProd) {
                log.info("Running Flyway repair (non-prod) to synchronize checksums...");
                flyway.repair();
            }
            log.info("Running Flyway migrate with validation enabled...");
            flyway.migrate();
        };
    }
}
