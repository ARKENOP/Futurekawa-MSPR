package com.futurekawa.backendcentral.fanout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.futurekawa.backendcentral.circuitbreaker.CountryCircuitBreakers;
import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.fanout.FanoutResult.CountrySuccess;
import com.futurekawa.backendcentral.registry.CountryRegistry;
import com.futurekawa.backendcentral.registry.LocalBackendDescriptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Exécute un appel donné sur tous les backends locaux en parallèle, isolé par un
 * CircuitBreaker par pays : un pays en panne (circuit ouvert, timeout, erreur réseau)
 * est simplement omis du résultat plutôt que de faire échouer tout le fan-out (§5).
 */
@Component
public class CountryFanoutExecutor {

    private static final Logger log = LoggerFactory.getLogger(CountryFanoutExecutor.class);

    private final CountryRegistry countryRegistry;
    private final CountryCircuitBreakers circuitBreakers;
    private final ExecutorService executor;

    public CountryFanoutExecutor(CountryRegistry countryRegistry, CountryCircuitBreakers circuitBreakers,
                                  ExecutorService fanoutTaskExecutor) {
        this.countryRegistry = countryRegistry;
        this.circuitBreakers = circuitBreakers;
        this.executor = fanoutTaskExecutor;
    }

    public <T> FanoutResult<T> execute(Function<LocalBackendClient, T> call) {
        List<LocalBackendDescriptor> descriptors = countryRegistry.all();

        List<CompletableFuture<Optional<CountrySuccess<T>>>> futures = descriptors.stream()
                .map(descriptor -> CompletableFuture.supplyAsync(() -> attempt(descriptor, call), executor))
                .toList();

        List<CountrySuccess<T>> successes = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            Optional<CountrySuccess<T>> result = futures.get(i).join();
            if (result.isPresent()) {
                successes.add(result.get());
            } else {
                unavailable.add(descriptors.get(i).codePays());
            }
        }
        return new FanoutResult<>(successes, unavailable);
    }

    private <T> Optional<CountrySuccess<T>> attempt(LocalBackendDescriptor descriptor, Function<LocalBackendClient, T> call) {
        String codePays = descriptor.codePays();
        CircuitBreaker circuitBreaker = circuitBreakers.forCountry(codePays);
        LocalBackendClient client = countryRegistry.client(codePays).orElseThrow();
        try {
            T data = circuitBreaker.executeSupplier(() -> call.apply(client));
            return Optional.of(new CountrySuccess<>(codePays, descriptor.nomPays(), data));
        } catch (Exception e) {
            log.warn("Backend local indisponible pour {} : {}", codePays, e.getMessage());
            return Optional.empty();
        }
    }
}
