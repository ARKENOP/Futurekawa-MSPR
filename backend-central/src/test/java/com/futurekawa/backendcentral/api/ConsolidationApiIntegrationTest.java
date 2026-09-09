package com.futurekawa.backendcentral.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.futurekawa.backendcentral.AbstractIntegrationTest;
import com.futurekawa.backendcentral.support.StubLocalBackends;

/**
 * The consolidated read API, end to end over HTTP: country grouping, the country
 * names discovery fills in, and the FIFO order the countries return.
 */
class ConsolidationApiIntegrationTest extends AbstractIntegrationTest {
    private ResponseEntity<String> get(String uri) {
        return client.get().uri(uri).retrieve().toEntity(String.class);
    }

    @Test
    void listsEveryDeployedCountry() {
        ResponseEntity<String> response = get("/api/v1/pays");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"codePays\":\"BR\"", "\"codePays\":\"EC\"");
        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isNull();
    }

    @Test
    void exposesEachCountryThresholdsSoTheFrontendNeedsNoHardcodedValues() {
        ResponseEntity<String> response = get("/api/v1/pays");

        assertThat(response.getBody())
                .contains("\"temperatureIdealeC\":29.0")
                .contains("\"toleranceTemperatureC\":3.0");
    }

    @Test
    void groupsLotsByCountryWithTheCountryNameAndAPage() {
        ResponseEntity<String> response = get("/api/v1/lots?page=0&size=20");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"codePays\":\"BR\"", "\"nomPays\":\"Brésil\"")
                .contains("\"codePays\":\"EC\"", "\"nomPays\":\"Équateur\"")
                .contains("\"totalElements\":2");
    }

    @Test
    void keepsTheOldestLotFirstSoFifoSurvivesConsolidation() {
        String body = get("/api/v1/lots?page=0&size=20").getBody();

        assertThat(body.indexOf("BR-2025-0001")).isLessThan(body.indexOf("BR-2026-0002"));
    }

    @Test
    void forwardsTheStatusFilterToEveryCountry() {
        String body = get("/api/v1/lots?statutLot=PERIME").getBody();

        assertThat(body).contains("BR-2025-0001").doesNotContain("BR-2026-0002");
    }

    @Test
    void groupsAlertesByCountry() {
        ResponseEntity<String> response = get("/api/v1/alertes?page=0&size=20");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"codePays\":\"BR\"", "CONDITION_NON_IDEALE", "LOT_TROP_ANCIEN");
    }

    @Test
    void forwardsTheAlerteStatusAndTypeFilters() {
        assertThat(get("/api/v1/alertes?statutAlerte=OUVERTE").getBody())
                .contains("CONDITION_NON_IDEALE").doesNotContain("LOT_TROP_ANCIEN");
        assertThat(get("/api/v1/alertes?typeAlerte=LOT_TROP_ANCIEN").getBody())
                .contains("LOT_TROP_ANCIEN").doesNotContain("CONDITION_NON_IDEALE");
    }

    @Test
    void groupsExploitationsAndEntrepotsByCountry() {
        assertThat(get("/api/v1/exploitations").getBody())
                .contains("Exploitation BR", "Exploitation EC");
        assertThat(get("/api/v1/entrepots").getBody())
                .contains("Entrepôt 1 BR", "Entrepôt 2 BR", "Entrepôt 1 EC");
    }

    @Test
    void filtersEntrepotsByExploitation() {
        String body = get("/api/v1/entrepots?exploitationId=1").getBody();

        assertThat(body).contains("Entrepôt 1 BR").doesNotContain("Entrepôt 2 BR");
    }

    @Test
    void readsOneCountrysLotThroughItsCountryCode() {
        ResponseEntity<String> response = get("/api/v1/lots/BR/7");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("BR-LOT-7");
    }

    @Test
    void readsTheMeasureHistoryOfOneEntrepot() {
        ResponseEntity<String> response = get("/api/v1/entrepots/BR/1/mesures?page=0&size=20");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"temperatureC\":29.0", "\"totalElements\":3");
    }

    @Test
    void narrowsTheMeasureHistoryToATimeWindow() {
        String body = get("/api/v1/entrepots/BR/1/mesures"
                + "?from=2026-06-17T08:15:00&to=2026-06-17T08:45:00").getBody();

        assertThat(body).contains("\"totalElements\":1").contains("\"temperatureC\":31.0");
    }

    @Test
    void readsTheLatestMeasureOfOneEntrepot() {
        assertThat(get("/api/v1/entrepots/BR/1/mesures/latest").getBody())
                .contains("\"temperatureC\":36.0");
    }

    @Test
    void queriesOnlyTheCountryAskedForOnAUnitaryRead() {
        StubLocalBackends.of("BR").reset();
        StubLocalBackends.of("EC").reset();

        get("/api/v1/lots/BR/7");

        assertThat(StubLocalBackends.of("BR").nombreDAppels()).isEqualTo(1);
        assertThat(StubLocalBackends.of("EC").nombreDAppels()).isZero();
    }
}
