package com.futurekawa.backendlocal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the FutureKawa Backend Local application.
 * <p>
 * A single Spring Boot codebase deployed per country. Country-specific
 * configuration (thresholds, tolerances, MQTT topics, etc.) is injected
 * via environment variables defined in the {@code .env} file.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class BackendLocalApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendLocalApplication.class, args);
    }
}
