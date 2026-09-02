package com.futurekawa.backendcentral;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.futurekawa.backendcentral.support.StubLocalBackendConfig;
import com.futurekawa.backendcentral.support.StubLocalBackends;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Drives the central over real HTTP, with the country backends stubbed in memory.
 *
 * <p>Same shape as backend-local's own {@code AbstractIntegrationTest}: a plain
 * {@link RestClient} against a random port, with error statuses returned rather than
 * thrown so tests can assert on them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(StubLocalBackendConfig.class)
public abstract class AbstractIntegrationTest {
    @LocalServerPort
    protected int port;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    protected RestClient client;

    /**
     * The stubs and the circuit breakers both live as long as the application
     * context, which is shared across the whole suite. A test that opens BR's
     * circuit would otherwise leak a 503 into the next one, so both are wound back
     * before each test.
     */
    @BeforeEach
    void resetEnvironment() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {  })
                .build();
        StubLocalBackends.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }
}
