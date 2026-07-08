package com.futurekawa.backendcentral.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LocalBackendClientFactory implements LocalBackendClient.Factory {

    private final RestClient.Builder builder;

    public LocalBackendClientFactory(RestClient.Builder localBackendRestClientBuilder) {
        this.builder = localBackendRestClientBuilder;
    }

    @Override
    public LocalBackendClient create(String baseUrl) {
        return new RestClientLocalBackendClient(builder.clone(), baseUrl);
    }
}
