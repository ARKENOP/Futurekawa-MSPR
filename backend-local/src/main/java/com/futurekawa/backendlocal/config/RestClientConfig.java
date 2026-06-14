package com.futurekawa.backendlocal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides a {@link RestClient.Builder} bean.
 *
 * <p>Spring Boot 4 does not auto-register a prototype {@code RestClient.Builder}
 * in this configuration, so the Odoo RPC client (which injects one) has no
 * candidate. This supplies a plain builder. Consumers set their own base URL.</p>
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
