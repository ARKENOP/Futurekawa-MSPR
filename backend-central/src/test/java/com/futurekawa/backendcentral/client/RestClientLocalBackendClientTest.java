package com.futurekawa.backendcentral.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.StatutLot;
import com.futurekawa.lib.enums.TypeAlerte;

/**
 * The URLs the central actually dials, and how it reads the answers back.
 *
 * <p>Worth pinning precisely: a filter the central forwards under the wrong
 * parameter name is dropped in silence by the country backend — the response is a
 * valid, unfiltered page, so nothing fails and the operator sees the wrong lots.
 * That exact class of bug has already occurred in this project.
 */
class RestClientLocalBackendClientTest {
    private static final String BASE = "http://backend-local-br.test:8081";

    private MockRestServiceServer server;
    private LocalBackendClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientLocalBackendClient(builder, BASE);
    }

    private void expect(String uri, String body) {
        server.expect(requestTo(BASE + uri))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expect(String uri, HttpMethod httpMethod, String body) {
        server.expect(requestTo(BASE + uri)).andExpect(method(httpMethod))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    /** A page as backend-local's {@code Page<T>} serialises it. */
    private static String page(String content) {
        return """
                {"content":[%s],"totalElements":1,"totalPages":1,"number":0,"size":20,
                 "numberOfElements":1,"first":true,"last":true,"empty":false}
                """.formatted(content);
    }

    private static final String LOT_JSON = """
            {"id":7,"referenceLot":"BR-2026-0001","dateEntreeStockage":"2026-03-01",
             "dateRecolte":"2026-02-01","statutLot":"CONFORME","qualiteLot":"Arabica AA",
             "exploitationId":1,"entrepotId":2,"paysId":1,"ancienneteJours":120}
            """;

    private static final String ALERTE_JSON = """
            {"id":5,"typeAlerte":"CONDITION_NON_IDEALE","niveau":"CRITIQUE",
             "statutAlerte":"OUVERTE","messageDescription":"Dérive",
             "dateHeureCreation":"2026-06-17T08:32:00","dateHeureCloture":null,
             "entrepotId":1,"lotId":null,"paysId":1}
            """;

    private static final String MESURE_JSON = """
            {"id":3,"idCapteur":"capteur-br","dateHeureMesure":"2026-06-17T09:00:00",
             "temperatureC":36.0,"humiditePourcent":55.0,"entrepotId":1,"lotId":null}
            """;

    @Test
    void readsTheCountryIdentityAndItsThresholds() {
        expect("/api/v1/pays", """
                {"id":1,"codePays":"BR","nomPays":"Brésil","temperatureIdealeC":29.0,
                 "humiditeIdealePourcent":55.0,"toleranceTemperatureC":3.0,
                 "toleranceHumiditePourcent":2.0,"estActif":true}
                """);

        var pays = client.getPays();

        server.verify();
        assertThat(pays.codePays()).isEqualTo("BR");
        assertThat(pays.nomPays()).isEqualTo("Brésil");
        assertThat(pays.toleranceTemperatureC()).isEqualByComparingTo("3.0");
    }

    @Test
    void readsTheExploitationList() {
        expect("/api/v1/exploitations", """
                [{"id":1,"nomExploitation":"Fazenda","localisation":"MG",
                  "responsableEmail":"responsable.br@futurekawa.local","estActive":true,
                  "paysId":1,"codePays":"BR"}]
                """);

        var exploitations = client.getExploitations();

        server.verify();
        assertThat(exploitations).hasSize(1);
        assertThat(exploitations.getFirst().responsableEmail())
                .isEqualTo("responsable.br@futurekawa.local");
    }

    @Test
    void omitsTheExploitationFilterWhenNoneIsAskedFor() {
        expect("/api/v1/entrepots", "[]");

        client.getEntrepots(null);

        server.verify();
    }

    @Test
    void sendsTheExploitationFilterWhenGiven() {
        expect("/api/v1/entrepots?exploitationId=4", "[]");

        client.getEntrepots(4L);

        server.verify();
    }

    @Test
    void readsOneEntrepotById() {
        expect("/api/v1/entrepots/2", """
                {"id":2,"nomEntrepot":"Entrepôt Sul","localisation":"Hangar B",
                 "capaciteMax":3000,"statutEntrepot":"actif","exploitationId":1,"paysId":1}
                """);

        assertThat(client.getEntrepot(2L).nomEntrepot()).isEqualTo("Entrepôt Sul");
        server.verify();
    }

    @Test
    void alwaysSendsPaginationOnLots() {
        expect("/api/v1/lots?page=2&size=50", page(LOT_JSON));

        var lots = client.getLots(null, 2, 50);

        server.verify();
        assertThat(lots.content()).hasSize(1);
        assertThat(lots.totalElements()).isEqualTo(1);
        assertThat(lots.content().getFirst().referenceLot()).isEqualTo("BR-2026-0001");
        assertThat(lots.content().getFirst().dateEntreeStockage())
                .isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void sendsTheLotStatusFilterOnlyWhenSet() {
        expect("/api/v1/lots?page=0&size=20&statutLot=PERIME", page(LOT_JSON));
        client.getLots(StatutLot.PERIME, 0, 20);
        server.verify();
    }

    @Test
    void omitsTheLotStatusFilterWhenUnset() {
        expect("/api/v1/lots?page=0&size=20", page(LOT_JSON));
        client.getLots(null, 0, 20);
        server.verify();
    }

    @Test
    void readsOneLotById() {
        expect("/api/v1/lots/7", LOT_JSON);

        assertThat(client.getLot(7L).ancienneteJours()).isEqualTo(120);
        server.verify();
    }

    @Test
    void sendsBothAlerteFiltersWhenSet() {
        expect("/api/v1/alertes?page=0&size=20&statutAlerte=OUVERTE"
                + "&typeAlerte=CONDITION_NON_IDEALE", page(ALERTE_JSON));

        client.getAlertes(StatutAlerte.OUVERTE, TypeAlerte.CONDITION_NON_IDEALE, 0, 20);

        server.verify();
    }

    @Test
    void sendsOnlyTheAlerteFilterThatIsSet() {
        expect("/api/v1/alertes?page=0&size=20&typeAlerte=LOT_TROP_ANCIEN", page(ALERTE_JSON));

        client.getAlertes(null, TypeAlerte.LOT_TROP_ANCIEN, 0, 20);

        server.verify();
    }

    @Test
    void readsOneAlerteById() {
        expect("/api/v1/alertes/5", ALERTE_JSON);

        var alerte = client.getAlerte(5L);

        server.verify();
        assertThat(alerte.statutAlerte()).isEqualTo(StatutAlerte.OUVERTE);
        assertThat(alerte.dateHeureCreation()).isEqualTo(LocalDateTime.of(2026, 6, 17, 8, 32));
        assertThat(alerte.dateHeureCloture()).isNull();
    }

    @Test
    void omitsTheTimeWindowOnMesuresWhenNotGiven() {
        expect("/api/v1/entrepots/1/mesures?page=0&size=20", page(MESURE_JSON));

        client.getMesures(1L, null, null, 0, 20);

        server.verify();
    }

    @Test
    void sendsTheTimeWindowOnMesuresWhenGiven() {
        expect("/api/v1/entrepots/1/mesures?page=0&size=20"
                + "&from=2026-06-17T08:00&to=2026-06-17T09:00", page(MESURE_JSON));

        client.getMesures(1L, LocalDateTime.of(2026, 6, 17, 8, 0),
                LocalDateTime.of(2026, 6, 17, 9, 0), 0, 20);

        server.verify();
    }

    @Test
    void readsTheLatestMesureOfAnEntrepot() {
        expect("/api/v1/entrepots/1/mesures/latest", MESURE_JSON);

        var mesure = client.getLatestMesure(1L);

        server.verify();
        assertThat(mesure.temperatureC()).isEqualByComparingTo("36.0");
        assertThat(mesure.idCapteur()).isEqualTo("capteur-br");
    }

    @Test
    void createsALotWithAPost() {
        expect("/api/v1/lots", HttpMethod.POST, LOT_JSON);

        var lot = client.createLot(new CreateLotRequest("BR-2026-0001",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), "Arabica AA", 1L, 2L));

        server.verify();
        assertThat(lot.id()).isEqualTo(7L);
    }

    @Test
    void updatesALotStatusWithAPatch() {
        expect("/api/v1/lots/7", HttpMethod.PATCH, LOT_JSON);

        client.updateLot(7L, new UpdateLotRequest(StatutLot.EN_ALERTE));

        server.verify();
    }

    @Test
    void updatesAnAlerteStatusWithAPatch() {
        expect("/api/v1/alertes/5", HttpMethod.PATCH, ALERTE_JSON);

        client.updateAlerte(5L, new UpdateAlerteRequest(StatutAlerte.CLOTUREE));

        server.verify();
    }

    @Test
    void letsTheCountrys404SurfaceAsAClientError() {
        server.expect(requestTo(BASE + "/api/v1/lots/404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getLot(404L))
                .isInstanceOf(HttpClientErrorException.class);
        server.verify();
    }
}
