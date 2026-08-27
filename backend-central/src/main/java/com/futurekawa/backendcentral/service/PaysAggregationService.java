package com.futurekawa.backendcentral.service;

import org.springframework.stereotype.Service;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.fanout.CountryFanoutExecutor;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.lib.dto.response.PaysResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaysAggregationService {

    private final CountryFanoutExecutor fanoutExecutor;

    public FanoutResult<PaysResponse> listAll() {
        return fanoutExecutor.execute(LocalBackendClient::getPays);
    }
}
