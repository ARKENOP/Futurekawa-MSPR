package com.futurekawa.backendcentral.client;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.dto.response.AlerteResponse;
import com.futurekawa.lib.dto.response.EntrepotResponse;
import com.futurekawa.lib.dto.response.ExploitationResponse;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.dto.response.MesureStockageResponse;
import com.futurekawa.lib.dto.response.PaysResponse;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.StatutLot;
import com.futurekawa.lib.enums.TypeAlerte;

public class RestClientLocalBackendClient implements LocalBackendClient {
    private final RestClient restClient;

    public RestClientLocalBackendClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public PaysResponse getPays() {
        return restClient.get().uri("/api/v1/pays").retrieve().body(PaysResponse.class);
    }

    @Override
    public List<ExploitationResponse> getExploitations() {
        return restClient.get().uri("/api/v1/exploitations").retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<EntrepotResponse> getEntrepots(Long exploitationId) {
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
    public EntrepotResponse getEntrepot(Long id) {
        return restClient.get().uri("/api/v1/entrepots/{id}", id).retrieve().body(EntrepotResponse.class);
    }

    @Override
    public PageDto<LotResponse> getLots(StatutLot statutLot, int page, int size) {
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
    public LotResponse getLot(Long id) {
        return restClient.get().uri("/api/v1/lots/{id}", id).retrieve().body(LotResponse.class);
    }

    @Override
    public LotResponse createLot(CreateLotRequest request) {
        return restClient.post().uri("/api/v1/lots").body(request).retrieve().body(LotResponse.class);
    }

    @Override
    public LotResponse updateLot(Long id, UpdateLotRequest request) {
        return restClient.patch().uri("/api/v1/lots/{id}", id).body(request).retrieve().body(LotResponse.class);
    }

    @Override
    public PageDto<AlerteResponse> getAlertes(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size) {
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
    public AlerteResponse getAlerte(Long id) {
        return restClient.get().uri("/api/v1/alertes/{id}", id).retrieve().body(AlerteResponse.class);
    }

    @Override
    public AlerteResponse updateAlerte(Long id, UpdateAlerteRequest request) {
        return restClient.patch().uri("/api/v1/alertes/{id}", id).body(request).retrieve().body(AlerteResponse.class);
    }

    @Override
    public PageDto<MesureStockageResponse> getMesures(Long entrepotId, LocalDateTime from, LocalDateTime to,
                                                       int page, int size) {
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
    public MesureStockageResponse getLatestMesure(Long entrepotId) {
        return restClient.get().uri("/api/v1/entrepots/{entrepotId}/mesures/latest", entrepotId)
                .retrieve().body(MesureStockageResponse.class);
    }
}
