package com.futurekawa.backendlocal.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.futurekawa.backendlocal.AbstractIntegrationTest;

/**
 * Verifies the country endpoint reflects the row seeded by DataInitializer
 * from the test profile (COUNTRY_CODE=BR).
 */
class PaysApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void returnsSeededCountry() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/pays")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"codePays\":\"BR\"");
    }
}
