package com.futurekawa.backendcentral.client;

import java.time.LocalDateTime;
import java.util.List;

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

public interface LocalBackendClient {
    PaysResponse getPays();

    List<ExploitationResponse> getExploitations();

    List<EntrepotResponse> getEntrepots(Long exploitationId);

    EntrepotResponse getEntrepot(Long id);

    PageDto<LotResponse> getLots(StatutLot statutLot, int page, int size);

    LotResponse getLot(Long id);

    LotResponse createLot(CreateLotRequest request);

    LotResponse updateLot(Long id, UpdateLotRequest request);

    PageDto<AlerteResponse> getAlertes(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size);

    AlerteResponse getAlerte(Long id);

    AlerteResponse updateAlerte(Long id, UpdateAlerteRequest request);

    PageDto<MesureStockageResponse> getMesures(Long entrepotId, LocalDateTime from, LocalDateTime to,
                                               int page, int size);

    MesureStockageResponse getLatestMesure(Long entrepotId);

    interface Factory {
        LocalBackendClient create(String baseUrl);
    }
}
