package com.futurekawa.backendcentral.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.futurekawa.backendcentral.client.LocalBackendClient;

@Component
public class CountryRegistry {

    private final Map<String, LocalBackendDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, LocalBackendClient> clients = new ConcurrentHashMap<>();

    public CountryRegistry(LocalBackendProperties properties, LocalBackendClient.Factory clientFactory) {
        for (LocalBackendProperties.Entry entry : properties.locals()) {
            descriptors.put(entry.codePays(), new LocalBackendDescriptor(entry.codePays(), entry.url()));
            clients.put(entry.codePays(), clientFactory.create(entry.url()));
        }
    }

    public List<LocalBackendDescriptor> all() {
        return List.copyOf(descriptors.values());
    }

    public Optional<LocalBackendDescriptor> descriptor(String codePays) {
        return Optional.ofNullable(descriptors.get(codePays));
    }

    public Optional<LocalBackendClient> client(String codePays) {
        return Optional.ofNullable(clients.get(codePays));
    }
}
