package com.futurekawa.backendlocal.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
