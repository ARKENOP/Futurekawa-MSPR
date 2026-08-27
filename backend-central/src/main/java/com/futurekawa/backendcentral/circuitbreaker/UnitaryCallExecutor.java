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
            // A 4xx is the country answering correctly about this one resource — an
            // absent lot or an entrepot with no mesure yet. It says nothing about the
            // backend's health, so it is relayed as-is instead of becoming a 503.
            // resilience4j.ignoreExceptions keeps it out of the failure rate too.
            throw e;
        } catch (Exception e) {
            throw new LocalBackendUnavailableException(codePays, e);
        }
    }
}
