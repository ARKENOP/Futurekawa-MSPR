package com.futurekawa.backendcentral.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.futurekawa.backendcentral.client.LocalBackendClient;

/** Détient tous les backends locaux configurés et leur client HTTP associé. */
@Component
public class CountryRegistry {

    private final Map<String, LocalBackendDescriptor> descriptors;
    private final Map<String, LocalBackendClient> clients;

    public CountryRegistry(LocalBackendProperties properties, LocalBackendClient.Factory clientFactory) {
        this.descriptors = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
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

    public Map<String, LocalBackendClient> clientsByCode() {
        return descriptors.values().stream()
                .collect(Collectors.toMap(LocalBackendDescriptor::codePays, d -> clients.get(d.codePays())));
    }
}
