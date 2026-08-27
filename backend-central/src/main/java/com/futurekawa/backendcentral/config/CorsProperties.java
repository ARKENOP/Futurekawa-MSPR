package com.futurekawa.backendcentral.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "futurekawa.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
