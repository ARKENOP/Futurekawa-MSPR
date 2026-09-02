package com.futurekawa.backendcentral.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.PaysAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;
import com.futurekawa.lib.dto.response.PaysResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pays")
@RequiredArgsConstructor
public class PaysController {
    private final PaysAggregationService paysAggregationService;

    @GetMapping
    public ResponseEntity<List<PaysResponse>> listPays() {
        FanoutResult<PaysResponse> result = paysAggregationService.listAll();
        List<PaysResponse> body = result.successes().stream()
                .map(FanoutResult.CountrySuccess::data)
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }
}
