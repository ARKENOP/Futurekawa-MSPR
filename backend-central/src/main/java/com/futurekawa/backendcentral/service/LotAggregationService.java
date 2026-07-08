package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.enums.StatutLot;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.CreateLotRequest;
import com.futurekawa.backendcentral.dto.request.UpdateLotRequest;
import com.futurekawa.backendcentral.dto.response.Lot;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;

@Service
public class LotAggregationService {

    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public LotAggregationService(CountryFanoutExecutor fanoutExecutor, UnitaryCallExecutor unitaryCallExecutor) {
        this.fanoutExecutor = fanoutExecutor;
        this.unitaryCallExecutor = unitaryCallExecutor;
    }

    public FanoutResult<PageDto<Lot>> listAll(StatutLot statutLot, int page, int size) {
        return fanoutExecutor.execute(client -> client.getLots(statutLot, page, size));
    }

    public Lot getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getLot(id));
    }

    public Lot create(CreateLotRequest request) {
        return unitaryCallExecutor.call(request.codePays(), client -> client.createLot(request));
    }

    public Lot updateStatut(String codePays, Long id, UpdateLotRequest request) {
        return unitaryCallExecutor.call(codePays, client -> client.updateLot(id, request));
    }
}
