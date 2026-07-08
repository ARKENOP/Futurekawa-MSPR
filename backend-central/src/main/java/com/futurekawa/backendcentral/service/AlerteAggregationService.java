package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.circuitbreaker.UnitaryCallExecutor;
import com.futurekawa.backendcentral.dto.enums.StatutAlerte;
import com.futurekawa.backendcentral.dto.enums.TypeAlerte;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.UpdateAlerteRequest;
import com.futurekawa.backendcentral.dto.response.Alerte;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;

@Service
public class AlerteAggregationService {

    private final CountryFanoutExecutor fanoutExecutor;
    private final UnitaryCallExecutor unitaryCallExecutor;

    public AlerteAggregationService(CountryFanoutExecutor fanoutExecutor, UnitaryCallExecutor unitaryCallExecutor) {
        this.fanoutExecutor = fanoutExecutor;
        this.unitaryCallExecutor = unitaryCallExecutor;
    }

    public FanoutResult<PageDto<Alerte>> listAll(StatutAlerte statutAlerte, TypeAlerte typeAlerte, int page, int size) {
        return fanoutExecutor.execute(client -> client.getAlertes(statutAlerte, typeAlerte, page, size));
    }

    public Alerte getById(String codePays, Long id) {
        return unitaryCallExecutor.call(codePays, client -> client.getAlerte(id));
    }

    public Alerte updateStatut(String codePays, Long id, UpdateAlerteRequest request) {
        return unitaryCallExecutor.call(codePays, client -> client.updateAlerte(id, request));
    }
}
