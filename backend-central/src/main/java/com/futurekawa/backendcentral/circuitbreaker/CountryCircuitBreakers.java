package com.futurekawa.backendcentral.circuitbreaker;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Un CircuitBreaker Resilience4j par codePays, créé à la volée à partir de la config
 * "default" (application.yml). Les codes pays ne sont pas connus à la compilation donc
 * on ne peut pas utiliser @CircuitBreaker(name=...) avec un nom dynamique fiable.
 */
@Component
public class CountryCircuitBreakers {

    private final CircuitBreakerRegistry registry;

    public CountryCircuitBreakers(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    public CircuitBreaker forCountry(String codePays) {
        return registry.circuitBreaker(codePays);
    }
}
