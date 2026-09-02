package com.futurekawa.backendcentral.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.futurekawa.backendcentral.support.StubLocalBackendClient;

/**
 * Discovery fills in what the registry cannot know from configuration alone — the
 * country's display name and its thresholds — and is the only place a registry
 * mismatch gets caught.
 */
class CountryDiscoverySchedulerTest {
    private static CountryRegistry registry(String code, StubLocalBackendClient client) {
        return new CountryRegistry(
                new LocalBackendProperties(List.of(code + "=http://" + code.toLowerCase() + ".test:8081")),
                baseUrl -> client);
    }

    @Test
    void storesWhatEachCountryReportsAboutItself() {
        CountryRegistry registry = registry("BR", new StubLocalBackendClient("BR", "Brésil"));

        new CountryDiscoveryScheduler(registry).refresh();

        LocalBackendDescriptor descriptor = registry.descriptor("BR").orElseThrow();
        assertThat(descriptor.nomPays()).isEqualTo("Brésil");
        assertThat(descriptor.pays().temperatureIdealeC()).isEqualByComparingTo("29.0");
        assertThat(descriptor.pays().toleranceTemperatureC()).isEqualByComparingTo("3.0");
    }

    @Test
    void survivesACountryBeingDownAndKeepsServingTheOthers() {
        StubLocalBackendClient br = new StubLocalBackendClient("BR", "Brésil");
        br.tombeEnPanne();
        CountryRegistry registry = registry("BR", br);

        assertThatCode(() -> new CountryDiscoveryScheduler(registry).refresh())
                .doesNotThrowAnyException();
        assertThat(registry.descriptor("BR").orElseThrow().nomPays()).isEqualTo("BR");
    }

    @Test
    void keepsRunningWhenABackendIsRegisteredUnderTheWrongCode() {
        CountryRegistry registry = registry("BR", new StubLocalBackendClient("EC", "Équateur"));

        assertThatCode(() -> new CountryDiscoveryScheduler(registry).refresh())
                .doesNotThrowAnyException();
        assertThat(registry.descriptor("BR").orElseThrow().pays().codePays()).isEqualTo("EC");
    }

    @Test
    void runsOnceAtStartupSoNamesArePresentOnTheFirstRequest() {
        CountryRegistry registry = registry("BR", new StubLocalBackendClient("BR", "Brésil"));

        new CountryDiscoveryScheduler(registry).run(null);

        assertThat(registry.descriptor("BR").orElseThrow().nomPays()).isEqualTo("Brésil");
    }

    @Test
    void refreshesTheNameWhenACountryIsRenamed() {
        StubLocalBackendClient br = new StubLocalBackendClient("BR", "Brésil");
        CountryRegistry registry = registry("BR", br);
        CountryDiscoveryScheduler scheduler = new CountryDiscoveryScheduler(registry);
        scheduler.refresh();

        CountryRegistry renamed = registry("BR", new StubLocalBackendClient("BR", "Brasil"));
        new CountryDiscoveryScheduler(renamed).refresh();

        assertThat(renamed.descriptor("BR").orElseThrow().nomPays()).isEqualTo("Brasil");
    }
}
