package com.futurekawa.backendlocal.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.futurekawa.backendlocal.AbstractIntegrationTest;

/**
 * Read-side endpoints for measures and alerts, including the 404 paths.
 */
class MesureAlerteApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void latestMeasureReturns404WhenNone() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/entrepots/1/mesures/latest")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Resource Not Found");
    }

    @Test
    void measureHistoryReturnsEmptyPage() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/entrepots/1/mesures")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }

    @Test
    void alertListReturnsPage() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/alertes")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }

    @Test
    void missingAlertReturns404() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/alertes/999999")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
