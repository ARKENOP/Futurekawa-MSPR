package com.futurekawa.backendlocal.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Immutable record binding the country-specific storage thresholds from
 * {@code futurekawa.pays.*} properties (sourced from the {@code .env} file).
 *
 * <p>Uses constructor binding (Java record) following Spring Boot 4 best
 * practices for immutable configuration. Validated at startup via JSR-303
 * to fail-fast on missing or invalid environment variables.</p>
 *
 * <p>Used by {@code DataInitializer} to seed the {@code pays} table and
 * by the alerting engine to evaluate condition thresholds.</p>
 */
@Validated
@ConfigurationProperties(prefix = "futurekawa.pays")
public record PaysProperties(
        @NotBlank String code,
        @NotBlank String nom,
        @NotNull BigDecimal temperatureIdealeC,
        @NotNull BigDecimal humiditeIdealePourcent,
        @NotNull BigDecimal toleranceTemperatureC,
        @NotNull BigDecimal toleranceHumiditePourcent,
        @DefaultValue("365") int dureeMaxStockageJours
) {}

