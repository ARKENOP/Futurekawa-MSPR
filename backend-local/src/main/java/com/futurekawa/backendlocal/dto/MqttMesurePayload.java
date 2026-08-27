package com.futurekawa.backendlocal.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MqttMesurePayload(
        @JsonProperty("id_capteur") String idCapteur,
        @JsonProperty("temperature_c") BigDecimal temperatureC,
        @JsonProperty("humidite_pourcent") BigDecimal humiditePourcent,
        @JsonProperty("timestamp") Long timestamp
) {}
