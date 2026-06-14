package com.futurekawa.backendlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable record binding the MQTT broker connection properties from
 * {@code futurekawa.mqtt.*} (sourced from the {@code .env} file).
 *
 * <p>Uses constructor binding (Java record) following Spring Boot 4 best
 * practices for immutable configuration. Validated at startup via JSR-303.</p>
 *
 * <p>Used by {@code MqttConfig} to configure the Spring Integration
 * MQTT inbound channel adapter.</p>
 */
@Validated
@ConfigurationProperties(prefix = "futurekawa.mqtt")
public record MqttProperties(
        @NotBlank String brokerUrl,
        @NotBlank String topic,
        @NotBlank String clientId,
        @DefaultValue("1") int qos
) {}
