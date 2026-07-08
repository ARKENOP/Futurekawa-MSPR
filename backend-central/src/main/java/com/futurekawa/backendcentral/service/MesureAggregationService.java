package com.futurekawa.backendcentral.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.response.MesureStockage;

/** Une mesure cible un entrepôt unique -> un seul pays -> pas de fan-out (§4.6). */
@Service
public class MesureAggregationService {

    private final UnitaryCallExecutor unitaryCallExecutor;

    public MesureAggregationService(UnitaryCallExecutor unitaryCallExecutor) {
        this.unitaryCallExecutor = unitaryCallExecutor;
    }

    public PageDto<MesureStockage> getHistory(String codePays, Long entrepotId, LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        return unitaryCallExecutor.call(codePays, client -> client.getMesures(entrepotId, from, to, page, size));
    }

    public MesureStockage getLatest(String codePays, Long entrepotId) {
        return unitaryCallExecutor.call(codePays, client -> client.getLatestMesure(entrepotId));
    }
}
