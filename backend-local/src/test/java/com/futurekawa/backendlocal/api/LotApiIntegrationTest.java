package com.futurekawa.backendlocal.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurekawa.backendlocal.AbstractIntegrationTest;

class LotApiIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private long createLot(String reference) throws Exception {
        String body = """
                {"referenceLot":"%s","dateEntreeStockage":"2026-01-15",
                 "dateRecolte":"2025-12-01","qualiteLot":"AA",
                 "exploitationId":1,"entrepotId":1}
                """.formatted(reference);

        ResponseEntity<String> response = client.post()
                .uri("/api/v1/lots")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JSON.readTree(response.getBody()).get("id").asLong();
    }

    @Test
    void createsAndFetchesLot() throws Exception {
        String ref = "LOT-" + UUID.randomUUID();
        long id = createLot(ref);

        ResponseEntity<String> fetched = client.get()
                .uri("/api/v1/lots/" + id)
                .retrieve()
                .toEntity(String.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode node = JSON.readTree(fetched.getBody());
        assertThat(node.get("referenceLot").asText()).isEqualTo(ref);
        assertThat(node.get("statutLot").asText()).isEqualTo("CONFORME");
    }

    @Test
    void updatesLotStatus() throws Exception {
        long id = createLot("LOT-" + UUID.randomUUID());

        ResponseEntity<String> patched = client.patch()
                .uri("/api/v1/lots/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"statutLot\":\"PERIME\"}")
                .retrieve()
                .toEntity(String.class);

        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JSON.readTree(patched.getBody()).get("statutLot").asText()).isEqualTo("PERIME");
    }

    @Test
    void listReturnsPagedResult() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/lots?page=0&size=5")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }

    @Test
    void missingLotReturns404ProblemDetail() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/lots/999999")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Resource Not Found");
    }

    @Test
    void invalidBodyReturns400ValidationProblem() {
        ResponseEntity<String> response = client.post()
                .uri("/api/v1/lots")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"referenceLot\":\"\",\"exploitationId\":1,\"entrepotId\":1}")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("invalid_fields");
    }

    @Test
    void invalidSortPropertyReturns400() {
        ResponseEntity<String> response = client.get()
                .uri("/api/v1/lots?sort=doesNotExist,desc")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid 'sort'");
    }
}
