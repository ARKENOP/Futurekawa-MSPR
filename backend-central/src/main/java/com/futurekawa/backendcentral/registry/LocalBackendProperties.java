package com.futurekawa.backendcentral.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The backend-local instances this central aggregates, as `CODE=url` pairs.
 *
 * The number of countries is a deployment choice, not a property of the code: one
 * backend-local is deployed per country and declares its own identity through its
 * .env (COUNTRY_CODE / COUNTRY_NAME). The list is therefore open-ended and comes
 * entirely from configuration:
 *
 *   FUTUREKAWA_LOCALS=BR=http://backend-local-br:8081,EC=http://backend-local-ec:8081
 *
 * The code is declared alongside the URL so a country can be routed to before the
 * first discovery call has answered; CountryDiscoveryScheduler then checks that the
 * backend reports the same codePays.
 */
@ConfigurationProperties(prefix = "futurekawa")
public record LocalBackendProperties(List<String> locals) {
    public record Entry(String codePays, String url) {
    }

    /** Parsed, validated entries. Fails fast: a malformed registry is a deployment error. */
    public List<Entry> entries() {
        Map<String, Entry> byCode = new LinkedHashMap<>();
        List<Entry> parsed = new ArrayList<>();
        for (String raw : locals == null ? List.<String>of() : locals) {
            String candidate = raw == null ? "" : raw.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            int separator = candidate.indexOf('=');
            if (separator <= 0 || separator == candidate.length() - 1) {
                throw new IllegalStateException(
                        "Invalid futurekawa.locals entry '" + candidate + "'. Expected CODE=url, "
                                + "e.g. BR=http://backend-local-br:8081");
            }
            Entry entry = new Entry(
                    candidate.substring(0, separator).trim(),
                    candidate.substring(separator + 1).trim());
            Entry duplicate = byCode.putIfAbsent(entry.codePays(), entry);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Duplicate country code '" + entry.codePays() + "' in futurekawa.locals");
            }
            parsed.add(entry);
        }
        return List.copyOf(parsed);
    }
}
