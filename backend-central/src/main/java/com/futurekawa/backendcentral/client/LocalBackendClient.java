package com.futurekawa.backendcentral.client;

import java.time.LocalDateTime;
import java.util.List;

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

/** Appels HTTP typés vers un unique backend local, un client par pays (voir CountryRegistry). */
public interface LocalBackendClient {

    Pays getPays();

    List<Exploitation> getExploitations();

    List<Entrepot> getEntrepots(Long exploitationId);

    Entrepot getEntrepot(Long id);

    PageDto<Lot> getLots(StatutLot statutLot, int page, int size);

    Lot getLot(Long id);

    Lot createLot(CreateLotRequest request);

    Lot updateLot(Long id, UpdateLotRequest request);

    PageDto<Alerte> getAlertes(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size);

    Alerte getAlerte(Long id);

    Alerte updateAlerte(Long id, UpdateAlerteRequest request);

    PageDto<MesureStockage> getMesures(Long entrepotId, LocalDateTime from, LocalDateTime to, int page, int size);

    MesureStockage getLatestMesure(Long entrepotId);

    /** Construit un LocalBackendClient lié à l'URL de base d'un backend local donné. */
    interface Factory {
        LocalBackendClient create(String baseUrl);
    }
}
