package com.futurekawa.backendcentral.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.futurekawa.backendcentral.AbstractIntegrationTest;
import com.futurekawa.backendcentral.support.StubLocalBackends;

/**
 * Writes go to exactly one country. The country owns its data, so a lot created at
 * the siège has to land in the right backend and nowhere else — and the routing key
 * must not leak into the payload the country receives.
 */
class WriteRoutingApiIntegrationTest extends AbstractIntegrationTest {
    private ResponseEntity<String> post(String uri, String json) {
        return client.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON).body(json)
                .retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> patch(String uri, String json) {
        return client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON).body(json)
                .retrieve().toEntity(String.class);
    }

    private static String nouveauLot(String codePays, String reference) {
        return """
                {
                  "codePays": "%s",
                  "referenceLot": "%s",
                  "dateEntreeStockage": "2026-03-01",
                  "dateRecolte": "2026-02-01",
                  "qualiteLot": "Arabica AA",
                  "exploitationId": 1,
                  "entrepotId": 2
                }
                """.formatted(codePays, reference);
    }

    @Test
    void createsTheLotInTheCountryItNames() {
        ResponseEntity<String> response = post("/api/v1/lots", nouveauLot("EC", "EC-2026-0100"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(StubLocalBackends.of("EC").lotsCrees())
                .extracting(r -> r.referenceLot()).containsExactly("EC-2026-0100");
        assertThat(StubLocalBackends.of("BR").lotsCrees()).isEmpty();
    }

    @Test
    void forwardsTheSubmittedStorageDateUnchanged() {
        post("/api/v1/lots", nouveauLot("BR", "BR-2026-0101"));

        assertThat(StubLocalBackends.of("BR").lotsCrees().getFirst().dateEntreeStockage())
                .isEqualTo(java.time.LocalDate.of(2026, 3, 1));
    }

    @Test
    void doesNotForwardTheRoutingCountryToTheBackend() {
        post("/api/v1/lots", nouveauLot("BR", "BR-2026-0102"));

        assertThat(StubLocalBackends.of("BR").lotsCrees().getFirst().toString())
                .doesNotContain("codePays");
    }

    @Test
    void rejectsALotForACountryThatIsNotDeployed() {
        ResponseEntity<String> response = post("/api/v1/lots", nouveauLot("ZZ", "ZZ-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("unknown-country");
    }

    @Test
    void rejectsAnIncompleteLotAndNamesTheOffendingFields() {
        ResponseEntity<String> response = post("/api/v1/lots", """
                {"codePays": "BR", "qualiteLot": "Arabica"}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("validation-failed")
                .contains("referenceLot")
                .contains("dateEntreeStockage");
        assertThat(StubLocalBackends.of("BR").lotsCrees()).isEmpty();
    }

    @Test
    void answers503WhenTheTargetCountryIsUnreachable() {
        StubLocalBackends.of("BR").tombeEnPanne();

        ResponseEntity<String> response = post("/api/v1/lots", nouveauLot("BR", "BR-2026-0103"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("\"codePays\":\"BR\"");
    }

    @Test
    void updatesALotStatusInTheRightCountry() {
        ResponseEntity<String> response =
                patch("/api/v1/lots/BR/3", "{\"statutLot\": \"EN_ALERTE\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"statutLot\":\"EN_ALERTE\"");
    }

    @Test
    void closesAnAlerteInTheRightCountry() {
        ResponseEntity<String> response =
                patch("/api/v1/alertes/EC/1", "{\"statutAlerte\": \"CLOTUREE\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"statutAlerte\":\"CLOTUREE\"");
    }

    @Test
    void makesTheIntermediateNotifieeStatusReachable() {
        assertThat(patch("/api/v1/alertes/BR/1", "{\"statutAlerte\": \"NOTIFIEE\"}").getBody())
                .contains("\"statutAlerte\":\"NOTIFIEE\"");
    }

    @Test
    void rejectsAnUnknownStatusValueAsABadRequest() {
        ResponseEntity<String> response =
                patch("/api/v1/lots/BR/3", "{\"statutLot\": \"PAS_UN_STATUT\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("unreadable-body");
    }

    @Test
    void rejectsMalformedJsonAsABadRequest() {
        ResponseEntity<String> response = post("/api/v1/lots", "{ pas du json ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("unreadable-body");
    }

    @Test
    void rejectsAnUnknownFilterValueAsABadRequest() {
        ResponseEntity<String> response = client.get().uri("/api/v1/lots?statutLot=BOGUS")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("invalid-parameter")
                .contains("\"parameter\":\"statutLot\"");
    }
}
