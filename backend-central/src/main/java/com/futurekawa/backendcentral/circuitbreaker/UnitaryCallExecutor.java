package com.futurekawa.backendcentral.circuitbreaker;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.exception.LocalBackendUnavailableException;
import com.futurekawa.backendcentral.exception.UnknownCountryException;
import com.futurekawa.backendcentral.registry.CountryRegistry;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UnitaryCallExecutor {
    private final CountryRegistry countryRegistry;
    private final CountryCircuitBreakers circuitBreakers;

    public <T> T call(String codePays, Function<LocalBackendClient, T> call) {
        LocalBackendClient client = countryRegistry.client(codePays)
                .orElseThrow(() -> new UnknownCountryException(codePays));
        CircuitBreaker circuitBreaker = circuitBreakers.forCountry(codePays);
        try {
            return circuitBreaker.executeSupplier(() -> call.apply(client));
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new LocalBackendUnavailableException(codePays, e);
        }
    }
}
