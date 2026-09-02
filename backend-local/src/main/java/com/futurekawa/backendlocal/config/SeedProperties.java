package com.futurekawa.backendlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Whether {@link DataInitializer} seeds the deployment's reference data.
 *
 * <p>On by default: a country backend with no {@code Pays} record cannot answer
 * {@code /api/v1/pays}, and one with no entrepôt cannot ingest a measure. Turn it off
 * for a deployment whose reference data is provisioned elsewhere.
 */
@ConfigurationProperties(prefix = "futurekawa.seed")
public record SeedProperties(@DefaultValue("true") boolean enabled) {}
