package com.futurekawa.backendcentral.registry;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "futurekawa")
public record LocalBackendProperties(List<Entry> locals) {

    public record Entry(String codePays, String url) {
    }
}
