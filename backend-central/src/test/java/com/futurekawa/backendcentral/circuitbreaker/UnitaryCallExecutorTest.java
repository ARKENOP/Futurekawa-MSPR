package com.futurekawa.backendcentral.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.futurekawa.backendcentral.exception.LocalBackendUnavailableException;
import com.futurekawa.backendcentral.exception.UnknownCountryException;
import com.futurekawa.backendcentral.registry.CountryRegistry;
import com.futurekawa.backendcentral.registry.LocalBackendProperties;
import com.futurekawa.backendcentral.support.StubLocalBackendClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Single-country calls: the executor has to tell three situations apart, because the
 * frontend reacts differently to each — an unknown country (404), a resource the
 * country legitimately does not have (relay its 404), and a country that cannot be
 * reached (503).
 */
class UnitaryCallExecutorTest {
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private UnitaryCallExecutor executor;

    @BeforeEach
    void setUp() {
        CountryRegistry registry = new CountryRegistry(
                new LocalBackendProperties(List.of("BR=http://br.test:8081")),
                baseUrl -> new StubLocalBackendClient("BR", "Brésil"));
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .ignoreExceptions(HttpClientErrorException.class)
                .build());
        executor = new UnitaryCallExecutor(registry, new CountryCircuitBreakers(circuitBreakerRegistry));
    }

    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);
    }

    @Test
    void returnsWhatTheCountryAnswered() {
        String codePays = executor.call("BR", client -> client.getPays().codePays());

        assertThat(codePays).isEqualTo("BR");
    }

    @Test
    void rejectsACountryThatIsNotRegistered() {
        assertThatThrownBy(() -> executor.call("ZZ", client -> "x"))
                .isInstanceOf(UnknownCountryException.class)
                .hasMessageContaining("ZZ");
    }

    @Test
    void relaysA404FromTheCountryInsteadOfMaskingItAsAnOutage() {
        assertThatThrownBy(() -> executor.call("BR", client -> {
            throw notFound();
        })).isInstanceOf(HttpClientErrorException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(HttpClientErrorException.class))
                .extracting(HttpClientErrorException::getStatusCode)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reportsAnUnreachableCountryAsUnavailable() {
        assertThatThrownBy(() -> executor.call("BR", client -> {
            throw new ResourceAccessException("connection refused");
        })).isInstanceOf(LocalBackendUnavailableException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(LocalBackendUnavailableException.class))
                .extracting(LocalBackendUnavailableException::getCodePays)
                .isEqualTo("BR");
    }

    @Test
    void repeated404sDoNotOpenTheCircuit() {
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> executor.call("BR", client -> {
                throw notFound();
            })).isInstanceOf(HttpClientErrorException.class);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("BR").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        String suivant = executor.call("BR", client -> "toujours joignable");
        assertThat(suivant).isEqualTo("toujours joignable");
    }

    @Test
    void repeatedOutagesDoOpenTheCircuit() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> executor.call("BR", client -> {
                throw new ResourceAccessException("connection refused");
            })).isInstanceOf(LocalBackendUnavailableException.class);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("BR").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> executor.call("BR", client -> "never runs"))
                .isInstanceOf(LocalBackendUnavailableException.class);
    }
}
