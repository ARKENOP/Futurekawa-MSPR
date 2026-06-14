package com.futurekawa.backendlocal.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the MQTT message payload received from sensors.
 * Designed to map from JSON natively using Jackson.
 */
public record MqttMesurePayload(
        @JsonProperty("id_capteur") String idCapteur,
        @JsonProperty("temperature_c") BigDecimal temperatureC,
        @JsonProperty("humidite_pourcent") BigDecimal humiditePourcent,
        @JsonProperty("timestamp") Long timestamp
) {}
