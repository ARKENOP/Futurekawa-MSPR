package com.futurekawa.backendcentral.client;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.futurekawa.backendcentral.dto.enums.StatutAlerte;
import com.futurekawa.backendcentral.dto.enums.StatutLot;
import com.futurekawa.backendcentral.dto.enums.TypeAlerte;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.CreateLotRequest;
import com.futurekawa.backendcentral.dto.request.UpdateAlerteRequest;
import com.futurekawa.backendcentral.dto.request.UpdateLotRequest;
import com.futurekawa.backendcentral.dto.response.Alerte;
import com.futurekawa.backendcentral.dto.response.Entrepot;
import com.futurekawa.backendcentral.dto.response.Exploitation;
import com.futurekawa.backendcentral.dto.response.Lot;
import com.futurekawa.backendcentral.dto.response.MesureStockage;
import com.futurekawa.backendcentral.dto.response.Pays;

/** Implémentation RestClient (bloquante) de LocalBackendClient, une instance par backend local. */
public class RestClientLocalBackendClient implements LocalBackendClient {

    private final RestClient restClient;

    public RestClientLocalBackendClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Pays getPays() {
        return restClient.get().uri("/api/v1/pays").retrieve().body(Pays.class);
    }

    @Override
    public List<Exploitation> getExploitations() {
        return restClient.get().uri("/api/v1/exploitations").retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Entrepot> getEntrepots(Long exploitationId) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/api/v1/entrepots");
                    if (exploitationId != null) {
                        builder = builder.queryParam("exploitationId", exploitationId);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Entrepot getEntrepot(Long id) {
        return restClient.get().uri("/api/v1/entrepots/{id}", id).retrieve().body(Entrepot.class);
    }

    @Override
    public PageDto<Lot> getLots(StatutLot statutLot, int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/api/v1/lots")
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (statutLot != null) {
                        builder = builder.queryParam("statutLot", statutLot);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Lot getLot(Long id) {
        return restClient.get().uri("/api/v1/lots/{id}", id).retrieve().body(Lot.class);
    }

    @Override
    public Lot createLot(CreateLotRequest request) {
        LocalCreateLotPayload payload = new LocalCreateLotPayload(
                request.referenceLot(),
                request.dateEntreeStockage(),
                request.dateRecolte(),
                request.qualiteLot(),
                request.exploitationId(),
                request.entrepotId());
        return restClient.post().uri("/api/v1/lots").body(payload).retrieve().body(Lot.class);
    }

    @Override
    public Lot updateLot(Long id, UpdateLotRequest request) {
        return restClient.patch().uri("/api/v1/lots/{id}", id).body(request).retrieve().body(Lot.class);
    }

    @Override
    public PageDto<Alerte> getAlertes(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/api/v1/alertes")
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (statutAlerte != null) {
                        builder = builder.queryParam("statutAlerte", statutAlerte);
                    }
                    if (typeAlerte != null) {
                        builder = builder.queryParam("typeAlerte", typeAlerte);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Alerte getAlerte(Long id) {
        return restClient.get().uri("/api/v1/alertes/{id}", id).retrieve().body(Alerte.class);
    }

    @Override
    public Alerte updateAlerte(Long id, UpdateAlerteRequest request) {
        return restClient.patch().uri("/api/v1/alertes/{id}", id).body(request).retrieve().body(Alerte.class);
    }

    @Override
    public PageDto<MesureStockage> getMesures(Long entrepotId, LocalDateTime from, LocalDateTime to, int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/api/v1/entrepots/{entrepotId}/mesures")
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (from != null) {
                        builder = builder.queryParam("from", from);
                    }
                    if (to != null) {
                        builder = builder.queryParam("to", to);
                    }
                    return builder.build(entrepotId);
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public MesureStockage getLatestMesure(Long entrepotId) {
        return restClient.get().uri("/api/v1/entrepots/{entrepotId}/mesures/latest", entrepotId)
                .retrieve().body(MesureStockage.class);
    }
}
