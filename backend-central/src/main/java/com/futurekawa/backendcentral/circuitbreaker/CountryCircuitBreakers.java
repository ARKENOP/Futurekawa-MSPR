package com.futurekawa.backendcentral.circuitbreaker;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
// Country codes are not known at compile time, so @CircuitBreaker(name = ...) cannot
// be used: the breakers are created on demand from the "default" config instead.
public class CountryCircuitBreakers {

    private final CircuitBreakerRegistry registry;

    public CircuitBreaker forCountry(String codePays) {
        return registry.circuitBreaker(codePays);
    }
}
