package com.futurekawa.backendlocal.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurekawa.backendlocal.AbstractIntegrationTest;

class MesureAlerteApiIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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
    @Test
    void alertListFiltersByStatutAndType() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/alertes?statutAlerte=OUVERTE&typeAlerte=CONDITION_NON_IDEALE")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }

    @Test
    void measureHistoryAcceptsATimeWindow() throws Exception {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/entrepots/1/mesures?from=2020-01-01T00:00:00&to=2020-01-02T00:00:00")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JSON.readTree(response.getBody()).get("content")).isEmpty();
    }
}
