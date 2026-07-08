package com.futurekawa.backendcentral.registry;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lie futurekawa.locals[] (codePays + url attendus) depuis application.yml/.env. */
@ConfigurationProperties(prefix = "futurekawa")
public record LocalBackendProperties(List<Entry> locals) {

    public record Entry(String codePays, String url) {
    }
}
