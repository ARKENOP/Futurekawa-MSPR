package com.futurekawa.backendcentral.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.dto.envelope.CountryPageGroup;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.AlerteAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.dto.response.AlerteResponse;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/alertes")
@RequiredArgsConstructor
public class AlerteController {
    private final AlerteAggregationService alerteAggregationService;

    @GetMapping
    public ResponseEntity<List<CountryPageGroup<AlerteResponse>>> listAlertes(
            @RequestParam(required = false) StatutAlerte statutAlerte,
            @RequestParam(required = false) TypeAlerte typeAlerte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FanoutResult<PageDto<AlerteResponse>> result = alerteAggregationService.listAll(statutAlerte, typeAlerte, page, size);
        List<CountryPageGroup<AlerteResponse>> body = result.successes().stream()
                .map(success -> new CountryPageGroup<>(success.codePays(), success.nomPays(), success.data()))
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }

    @GetMapping("/{codePays}/{id}")
    public AlerteResponse getAlerte(@PathVariable String codePays, @PathVariable Long id) {
        return alerteAggregationService.getById(codePays, id);
    }

    @PatchMapping("/{codePays}/{id}")
    public AlerteResponse updateAlerteStatut(@PathVariable String codePays, @PathVariable Long id,
                                      @Valid @RequestBody UpdateAlerteRequest request) {
        return alerteAggregationService.updateStatut(codePays, id, request);
    }
}
