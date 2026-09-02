package com.futurekawa.backendcentral.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.futurekawa.backendcentral.AbstractIntegrationTest;
import com.futurekawa.backendcentral.support.StubLocalBackends;

/**
 * What the siège sees when a country misbehaves. This is the argument for the
 * distributed architecture, so it is asserted at the API boundary rather than on the
 * fan-out alone: one country down must degrade the payload and say so, never fail
 * the request or empty the dashboard.
 */
class ResilienceApiIntegrationTest extends AbstractIntegrationTest {
    private ResponseEntity<String> get(String uri) {
        return client.get().uri(uri).retrieve().toEntity(String.class);
    }

    @Test
    void stillServesTheHealthyCountriesWhenOneIsDown() {
        StubLocalBackends.of("EC").tombeEnPanne();

        ResponseEntity<String> response = get("/api/v1/lots");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"codePays\":\"BR\"");
        assertThat(response.getBody()).doesNotContain("\"codePays\":\"EC\"");
        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isEqualTo("EC");
    }

    @Test
    void namesEveryDownCountryInTheHeader() {
        StubLocalBackends.of("BR").tombeEnPanne();
        StubLocalBackends.of("EC").tombeEnPanne();

        ResponseEntity<String> response = get("/api/v1/pays");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isEqualTo("BR,EC");
    }

    @Test
    void relaysTheCountrys404InsteadOfClaimingAnOutage() {
        StubLocalBackends.of("BR").declareLotAbsent(404L);

        ResponseEntity<String> response = get("/api/v1/lots/BR/404");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("local-backend-rejected");
    }

    @Test
    void relaysA404FromAnEntrepotWithNoMeasureYet() {
        StubLocalBackends.of("BR").declareEntrepotSansMesure(1L);

        assertThat(get("/api/v1/entrepots/BR/1/mesures/latest").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void repeated404sNeverDropAHealthyCountryFromTheConsolidatedApi() {
        StubLocalBackends.of("BR").declareEntrepotSansMesure(1L);
        for (int i = 0; i < 10; i++) {
            assertThat(get("/api/v1/entrepots/BR/1/mesures/latest").getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        ResponseEntity<String> lots = get("/api/v1/lots");

        assertThat(lots.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lots.getBody()).contains("\"codePays\":\"BR\"");
        assertThat(lots.getHeaders().getFirst("X-Unavailable-Countries")).isNull();
    }

    @Test
    void answers503WithTheCountryCodeWhenAUnitaryReadCannotReachIt() {
        StubLocalBackends.of("BR").tombeEnPanne();

        ResponseEntity<String> response = get("/api/v1/lots/BR/1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .contains("local-backend-unavailable")
                .contains("\"codePays\":\"BR\"");
    }

    @Test
    void answers404ForACountryThatIsNotDeployed() {
        ResponseEntity<String> response = get("/api/v1/lots/ZZ/1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("unknown-country").contains("\"codePays\":\"ZZ\"");
    }

    @Test
    void opensTheCircuitAfterRepeatedOutagesAndStopsCallingTheCountry() {
        StubLocalBackends.of("BR").tombeEnPanne();
        for (int i = 0; i < 5; i++) {
            assertThat(get("/api/v1/lots/BR/1").getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
        int appelsAvant = StubLocalBackends.of("BR").nombreDAppels();

        ResponseEntity<String> response = get("/api/v1/lots/BR/1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(StubLocalBackends.of("BR").nombreDAppels()).isEqualTo(appelsAvant);
    }

    @Test
    void oneCountrysOpenCircuitLeavesTheOtherUntouched() {
        StubLocalBackends.of("BR").tombeEnPanne();
        for (int i = 0; i < 5; i++) {
            get("/api/v1/lots/BR/1");
        }

        assertThat(get("/api/v1/lots/EC/1").getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
