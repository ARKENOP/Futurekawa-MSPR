package com.futurekawa.backendlocal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Base class for integration tests: boots the full application context on a
 * random port against an in-memory H2 database (PostgreSQL-compatibility mode,
 * configured in {@code application-test.yml}) — no Docker required.
 *
 * <p>Subclasses get a {@link RestClient} pre-configured with the server base URL
 * and a no-op error handler, so 4xx/5xx responses are returned for assertion
 * instead of being thrown.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected RestClient client;

    @BeforeEach
    void initRestClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { /* no throw */ })
                .build();
    }
}
