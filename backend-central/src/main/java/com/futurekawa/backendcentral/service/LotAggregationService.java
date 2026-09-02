package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.CreateLotRequest;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.enums.StatutLot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotAggregationService {
    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public FanoutResult<PageDto<LotResponse>> listAll(StatutLot statutLot, int page, int size) {
        return fanoutExecutor.execute(client -> client.getLots(statutLot, page, size));
    }

    public LotResponse getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getLot(id));
    }

    public LotResponse create(CreateLotRequest request) {
        return unitaryCallExecutor.call(request.codePays(), client -> client.createLot(request.toLocalRequest()));
    }

    public LotResponse updateStatut(String codePays, Long id, UpdateLotRequest request) {
        return unitaryCallExecutor.call(codePays, client -> client.updateLot(id, request));
    }
}
