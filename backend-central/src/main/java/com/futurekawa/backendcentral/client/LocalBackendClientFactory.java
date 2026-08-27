package com.futurekawa.backendcentral.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalBackendClientFactory implements LocalBackendClient.Factory {

    // Name matches the bean in RestClientConfig: renaming it changes the injection.
    private final RestClient.Builder localBackendRestClientBuilder;

    @Override
    public LocalBackendClient create(String baseUrl) {
        return new RestClientLocalBackendClient(localBackendRestClientBuilder.clone(), baseUrl);
    }
}
