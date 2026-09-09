package com.futurekawa.backendcentral.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder localBackendRestClientBuilder(ClientProperties clientProperties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(
                        Duration.ofMillis(clientProperties.connectTimeoutMs()),
                        Duration.ofMillis(clientProperties.readTimeoutMs()));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
