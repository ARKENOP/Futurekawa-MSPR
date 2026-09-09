package com.futurekawa.backendcentral.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.lib.dto.response.EntrepotResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrepotAggregationService {
    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public FanoutResult<List<EntrepotResponse>> listAll(Long exploitationId) {
        return fanoutExecutor.execute(client -> client.getEntrepots(exploitationId));
    }

    public EntrepotResponse getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getEntrepot(id));
    }
}
