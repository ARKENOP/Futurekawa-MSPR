package com.futurekawa.backendcentral.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.futurekawa.backendcentral.circuitbreaker.CountryCircuitBreakers;
import com.futurekawa.backendcentral.support.StubLocalBackendClient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

class CountryRegistryTest {
    private static CountryRegistry registryOf(String... locals) {
        return new CountryRegistry(
                new LocalBackendProperties(List.of(locals)),
                baseUrl -> new StubLocalBackendClient("XX", "Pays " + baseUrl));
    }

    @Test
    void registersOneDescriptorAndOneClientPerConfiguredCountry() {
        CountryRegistry registry = registryOf(
                "BR=http://br.test:8081", "EC=http://ec.test:8081");

        assertThat(registry.all()).hasSize(2);
        assertThat(registry.all()).extracting(LocalBackendDescriptor::codePays)
                .containsExactlyInAnyOrder("BR", "EC");
        assertThat(registry.client("BR")).isPresent();
        assertThat(registry.client("EC")).isPresent();
    }

    @Test
    void keepsEachCountryUrl() {
        assertThat(registryOf("BR=http://br.test:8081").descriptor("BR"))
                .get().extracting(LocalBackendDescriptor::url).isEqualTo("http://br.test:8081");
    }

    @Test
    void answersEmptyForACountryItDoesNotServe() {
        CountryRegistry registry = registryOf("BR=http://br.test:8081");

        assertThat(registry.descriptor("ZZ")).isEmpty();
        assertThat(registry.client("ZZ")).isEmpty();
    }

    @Test
    void fallsBackToTheCodeUntilDiscoveryHasNamedTheCountry() {
        LocalBackendDescriptor descriptor = new LocalBackendDescriptor("BR", "http://br.test:8081");

        assertThat(descriptor.pays()).isNull();
        assertThat(descriptor.nomPays()).isEqualTo("BR");
    }

    @Test
    void usesTheDiscoveredNameOnceKnown() {
        LocalBackendDescriptor descriptor = new LocalBackendDescriptor("BR", "http://br.test:8081");

        descriptor.updatePays(new StubLocalBackendClient("BR", "Brésil").getPays());

        assertThat(descriptor.nomPays()).isEqualTo("Brésil");
    }

    @Test
    void createsOneCircuitBreakerPerCountry() {
        CountryCircuitBreakers breakers =
                new CountryCircuitBreakers(CircuitBreakerRegistry.ofDefaults());

        assertThat(breakers.forCountry("BR")).isNotSameAs(breakers.forCountry("EC"));
        assertThat(breakers.forCountry("BR")).isSameAs(breakers.forCountry("BR"));
    }
}
