package com.futurekawa.backendcentral.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of the in-memory country backends the tests drive.
 *
 * <p>{@link com.futurekawa.backendcentral.registry.CountryRegistry} builds its
 * clients once, at context startup, from the URLs in {@code futurekawa.locals}.
 * The stubs therefore have to outlive a single test and be reachable by country
 * code, which is what this holder provides: the factory creates one stub per URL
 * during startup, and each test looks its country up by code and says how it
 * should behave.
 */
public final class StubLocalBackends {
    /** Must mirror {@code futurekawa.locals} in application-test.yml. */
    public static final Map<String, String> URLS = new LinkedHashMap<>(Map.of(
            "BR", "http://backend-local-br.test:8081",
            "EC", "http://backend-local-ec.test:8081"));

    private static final Map<String, String> NOMS = Map.of(
            "BR", "Brésil",
            "EC", "Équateur");

    private static final Map<String, StubLocalBackendClient> PAR_URL = new ConcurrentHashMap<>();

    private StubLocalBackends() {
    }

    /** Called by the test factory, once per configured URL, at context startup. */
    static StubLocalBackendClient parUrl(String url) {
        return PAR_URL.computeIfAbsent(url, u -> {
            String code = URLS.entrySet().stream()
                    .filter(e -> e.getValue().equals(u))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No stub country declared for URL " + u
                                    + ". Add it to StubLocalBackends.URLS and application-test.yml."));
            return new StubLocalBackendClient(code, NOMS.get(code));
        });
    }

    /** The stub backend serving this country code. */
    public static StubLocalBackendClient of(String codePays) {
        String url = URLS.get(codePays);
        if (url == null) {
            throw new IllegalArgumentException("Unknown stub country: " + codePays);
        }
        StubLocalBackendClient stub = PAR_URL.get(url);
        if (stub == null) {
            throw new IllegalStateException(
                    "Stub for " + codePays + " not created yet; the application context must start first.");
        }
        return stub;
    }

    /** Every country healthy again — call between tests, the stubs are shared. */
    public static void resetAll() {
        PAR_URL.values().forEach(StubLocalBackendClient::reset);
    }
}
