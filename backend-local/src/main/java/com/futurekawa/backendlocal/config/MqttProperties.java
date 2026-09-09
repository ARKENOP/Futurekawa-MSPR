package com.futurekawa.backendlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "futurekawa.mqtt")
public record MqttProperties(
        @NotBlank String brokerUrl,
        @NotBlank String topic,
        @NotBlank String clientId,
        @DefaultValue("1") int qos
) {}
