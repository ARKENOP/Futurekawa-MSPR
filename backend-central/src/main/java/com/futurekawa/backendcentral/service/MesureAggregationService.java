package com.futurekawa.backendcentral.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.lib.dto.response.MesureStockageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesureAggregationService {
    private final UnitaryCallExecutor unitaryCallExecutor;

    public PageDto<MesureStockageResponse> getHistory(String codePays, Long entrepotId, LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        return unitaryCallExecutor.call(codePays, client -> client.getMesures(entrepotId, from, to, page, size));
    }

    public MesureStockageResponse getLatest(String codePays, Long entrepotId) {
        return unitaryCallExecutor.call(codePays, client -> client.getLatestMesure(entrepotId));
    }
}
