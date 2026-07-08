package com.futurekawa.backendcentral.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.dto.response.Pays;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.PaysAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pays")
@RequiredArgsConstructor
public class PaysController {

    private final PaysAggregationService paysAggregationService;

    @GetMapping
    public ResponseEntity<List<Pays>> listPays() {
        FanoutResult<Pays> result = paysAggregationService.listAll();
        List<Pays> body = result.successes().stream()
                .map(FanoutResult.CountrySuccess::data)
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }
}
