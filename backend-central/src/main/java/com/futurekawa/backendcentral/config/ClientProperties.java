package com.futurekawa.backendcentral.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "futurekawa.client")
public record ClientProperties(int connectTimeoutMs, int readTimeoutMs) {
}
