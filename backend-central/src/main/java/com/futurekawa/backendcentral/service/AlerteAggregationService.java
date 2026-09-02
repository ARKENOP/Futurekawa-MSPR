package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.dto.response.AlerteResponse;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlerteAggregationService {
    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public FanoutResult<PageDto<AlerteResponse>> listAll(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size) {
        return fanoutExecutor.execute(client -> client.getAlertes(statutAlerte, typeAlerte, page, size));
    }

    public AlerteResponse getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getAlerte(id));
    }

    public AlerteResponse updateStatut(String codePays, Long id, UpdateAlerteRequest request) {
        return unitaryCallExecutor.call(codePays, client -> client.updateAlerte(id, request));
    }
}
