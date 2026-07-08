package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.dto.response.Pays;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;

@Service
public class PaysAggregationService {

    private final CountryFanoutExecutor fanoutExecutor;

    public PaysAggregationService(CountryFanoutExecutor fanoutExecutor) {
        this.fanoutExecutor = fanoutExecutor;
    }

    public FanoutResult<Pays> listAll() {
        return fanoutExecutor.execute(LocalBackendClient::getPays);
    }
}
