package com.futurekawa.backendcentral.fanout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.futurekawa.backendcentral.circuitbreaker.CountryCircuitBreakers;
import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.registry.CountryRegistry;
import com.futurekawa.backendcentral.registry.LocalBackendProperties;
import com.futurekawa.backendcentral.support.StubLocalBackendClient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * The fan-out is what makes the siège tolerant to a country going dark: it must
 * return whatever answered and name what did not, never fail as a whole.
 */
class CountryFanoutExecutorTest {
    private static final String URL_BR = "http://br.test:8081";
    private static final String URL_EC = "http://ec.test:8081";
    private static final String URL_CO = "http://co.test:8081";

    private StubLocalBackendClient br;
    private StubLocalBackendClient ec;
    private StubLocalBackendClient co;

    private ExecutorService executorService;
    private CountryFanoutExecutor fanout;

    @BeforeEach
    void setUp() {
        br = new StubLocalBackendClient("BR", "Brésil");
        ec = new StubLocalBackendClient("EC", "Équateur");
        co = new StubLocalBackendClient("CO", "Colombie");

        CountryRegistry registry = new CountryRegistry(
                new LocalBackendProperties(List.of(
                        "BR=" + URL_BR, "EC=" + URL_EC, "CO=" + URL_CO)),
                baseUrl -> switch (baseUrl) {
                    case URL_BR -> br;
                    case URL_EC -> ec;
                    case URL_CO -> co;
                    default -> throw new IllegalArgumentException(baseUrl);
                });

        executorService = Executors.newVirtualThreadPerTaskExecutor();
        fanout = new CountryFanoutExecutor(registry,
                new CountryCircuitBreakers(CircuitBreakerRegistry.ofDefaults()),
                executorService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void consolidatesEveryHealthyCountry() {
        FanoutResult<String> result = fanout.execute(client -> client.getPays().codePays());

        assertThat(result.unavailable()).isEmpty();
        assertThat(result.successes()).extracting(FanoutResult.CountrySuccess::codePays)
                .containsExactlyInAnyOrder("BR", "EC", "CO");
        assertThat(result.successes()).extracting(FanoutResult.CountrySuccess::data)
                .containsExactlyInAnyOrder("BR", "EC", "CO");
    }

    @Test
    void keepsTheHealthyCountriesWhenOneIsUnreachable() {
        ec.tombeEnPanne();

        FanoutResult<String> result = fanout.execute(client -> client.getPays().codePays());

        assertThat(result.unavailable()).containsExactly("EC");
        assertThat(result.successes()).extracting(FanoutResult.CountrySuccess::codePays)
                .containsExactlyInAnyOrder("BR", "CO");
    }

    @Test
    void namesEveryUnreachableCountry() {
        br.tombeEnPanne();
        ec.tombeEnPanne();
        co.tombeEnPanne();

        FanoutResult<String> result = fanout.execute(client -> client.getPays().codePays());

        assertThat(result.successes()).isEmpty();
        assertThat(result.unavailable()).containsExactlyInAnyOrder("BR", "EC", "CO");
    }

    @Test
    void pairsEachCountryCodeWithItsOwnResult() {
        FanoutResult<String> result = fanout.execute(
                client -> client.getExploitations().getFirst().codePays());

        assertThat(result.successes())
                .allSatisfy(success -> assertThat(success.data()).isEqualTo(success.codePays()));
    }

    @Test
    void carriesTheCountryNameAlongsideItsCode() {
        FanoutResult<String> result = fanout.execute(client -> client.getPays().codePays());

        assertThat(result.successes()).extracting(FanoutResult.CountrySuccess::nomPays)
                .doesNotContainNull();
    }

    @Test
    void excludesACountryThatRejectsTheRequestAndStillServesTheOthers() {
        FanoutResult<String> result = fanout.execute(client -> {
            if (client == ec) {
                throw HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, null, null);
            }
            return client.getPays().codePays();
        });

        assertThat(result.unavailable()).containsExactly("EC");
        assertThat(result.successes()).extracting(FanoutResult.CountrySuccess::codePays)
                .containsExactlyInAnyOrder("BR", "CO");
    }

    @Test
    void queriesEveryCountryExactlyOnce() {
        fanout.execute(LocalBackendClient::getPays);

        assertThat(br.nombreDAppels()).isEqualTo(1);
        assertThat(ec.nombreDAppels()).isEqualTo(1);
        assertThat(co.nombreDAppels()).isEqualTo(1);
    }

    @Test
    void surfacesNoCountriesWhenTheRegistryIsEmpty() {
        CountryFanoutExecutor empty = new CountryFanoutExecutor(
                new CountryRegistry(new LocalBackendProperties(List.of()), baseUrl -> br),
                new CountryCircuitBreakers(CircuitBreakerRegistry.ofDefaults()),
                executorService);

        FanoutResult<String> result = empty.execute(client -> client.getPays().codePays());

        assertThat(result.successes()).isEmpty();
        assertThat(result.unavailable()).isEmpty();
    }

    @Test
    void treatsAnUnexpectedRuntimeFailureAsAnOutage() {
        FanoutResult<String> result = fanout.execute(client -> {
            if (client == co) {
                throw new IllegalStateException("deserialisation blew up");
            }
            return client.getPays().codePays();
        });

        assertThat(result.unavailable()).containsExactly("CO");
        assertThat(result.successes()).hasSize(2);
    }

    @Test
    void aCountryRecoversOnTheNextCall() {
        ec.tombeEnPanne();
        FanoutResult<String> pendantLaPanne = fanout.execute(client -> client.getPays().codePays());
        assertThat(pendantLaPanne.unavailable()).containsExactly("EC");

        ec.redevientDisponible();

        FanoutResult<String> apres = fanout.execute(client -> client.getPays().codePays());
        assertThat(apres.unavailable()).isEmpty();
        assertThat(apres.successes()).hasSize(3);
    }
}
