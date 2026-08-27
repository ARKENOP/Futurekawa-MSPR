package com.futurekawa.backendcentral.fanout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.futurekawa.backendcentral.circuitbreaker.CountryCircuitBreakers;
import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.fanout.FanoutResult.CountrySuccess;
import com.futurekawa.backendcentral.registry.CountryRegistry;
import com.futurekawa.backendcentral.registry.LocalBackendDescriptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountryFanoutExecutor {

    private final CountryRegistry countryRegistry;
    private final CountryCircuitBreakers circuitBreakers;

    // Name matches the bean in AsyncConfig: renaming it changes the injection.
    private final ExecutorService fanoutTaskExecutor;

    public <T> FanoutResult<T> execute(Function<LocalBackendClient, T> call) {
        List<LocalBackendDescriptor> descriptors = countryRegistry.all();

        List<CompletableFuture<Optional<CountrySuccess<T>>>> futures = descriptors.stream()
                .map(descriptor -> CompletableFuture.supplyAsync(() -> attempt(descriptor, call), fanoutTaskExecutor))
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

    private <T> Optional<CountrySuccess<T>> attempt(LocalBackendDescriptor descriptor,
                                                     Function<LocalBackendClient, T> call) {
        String codePays = descriptor.codePays();
        CircuitBreaker circuitBreaker = circuitBreakers.forCountry(codePays);
        LocalBackendClient client = countryRegistry.client(codePays).orElseThrow();
        try {
            T data = circuitBreaker.executeSupplier(() -> call.apply(client));
            return Optional.of(new CountrySuccess<>(codePays, descriptor.nomPays(), data));
        } catch (HttpClientErrorException e) {
            // The country answered, it just rejected this request. Reporting it as
            // "unavailable" would hide a healthy backend, so log loudly and move on.
            log.warn("Backend local for {} rejected the request ({}); country reported empty",
                    codePays, e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Backend local unavailable for {}: {}", codePays, e.getMessage());
            return Optional.empty();
        }
    }
}
