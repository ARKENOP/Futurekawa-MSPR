package com.futurekawa.backendcentral.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.response.Entrepot;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;

@Service
public class EntrepotAggregationService {

    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public EntrepotAggregationService(CountryFanoutExecutor fanoutExecutor, UnitaryCallExecutor unitaryCallExecutor) {
        this.fanoutExecutor = fanoutExecutor;
        this.unitaryCallExecutor = unitaryCallExecutor;
    }

    public FanoutResult<List<Entrepot>> listAll(Long exploitationId) {
        return fanoutExecutor.execute(client -> client.getEntrepots(exploitationId));
    }

    public Entrepot getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getEntrepot(id));
    }
}
