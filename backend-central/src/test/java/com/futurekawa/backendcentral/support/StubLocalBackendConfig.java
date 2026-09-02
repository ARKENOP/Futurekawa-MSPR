package com.futurekawa.backendcentral.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.futurekawa.backendcentral.client.LocalBackendClient;

/**
 * Replaces the HTTP client factory with one handing out {@link StubLocalBackendClient}s.
 *
 * <p>{@code @Primary} so it wins over {@code LocalBackendClientFactory} when
 * {@code CountryRegistry} asks for a factory at startup. Everything downstream of
 * the client — fan-out, circuit breakers, envelopes, error translation, HTTP layer
 * — stays the production code.
 */
@TestConfiguration
public class StubLocalBackendConfig {
    @Bean
    @Primary
    public LocalBackendClient.Factory stubLocalBackendClientFactory() {
        return StubLocalBackends::parUrl;
    }
}
