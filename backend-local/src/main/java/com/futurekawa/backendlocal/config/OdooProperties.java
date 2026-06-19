package com.futurekawa.backendlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable record binding the Odoo connection properties from
 * {@code futurekawa.odoo.*} (sourced from the {@code .env} file).
 *
 * <p>Uses constructor binding (Java record) following Spring Boot 4 best
 * practices for immutable configuration. Validated at startup via JSR-303.</p>
 *
 * <p>Used by {@code OdooRpcClient} and {@code OdooQualityAlertService} to
 * authenticate and push quality-alert tickets via Odoo's external API.</p>
 */
@Validated
@ConfigurationProperties(prefix = "futurekawa.odoo")
public record OdooProperties(
        @NotBlank String url,
        @NotBlank String db,
        @NotBlank String apiUser,
        @NotBlank String apiKey
) {}
