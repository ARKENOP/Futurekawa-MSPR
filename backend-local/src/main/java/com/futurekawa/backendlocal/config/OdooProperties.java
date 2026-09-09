package com.futurekawa.backendlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "futurekawa.odoo")
public record OdooProperties(
        @NotBlank String url,
        @NotBlank String db,
        @NotBlank String apiUser,
        @NotBlank String apiKey
) {}
