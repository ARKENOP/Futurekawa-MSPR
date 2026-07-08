package com.futurekawa.backendcentral.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.dto.envelope.CountryGroup;
import com.futurekawa.backendcentral.dto.response.Entrepot;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.EntrepotAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/entrepots")
@RequiredArgsConstructor
public class EntrepotController {

    private final EntrepotAggregationService entrepotAggregationService;

    @GetMapping
    public ResponseEntity<List<CountryGroup<Entrepot>>> listEntrepots(
            @RequestParam(required = false) Long exploitationId) {
        FanoutResult<List<Entrepot>> result = entrepotAggregationService.listAll(exploitationId);
        List<CountryGroup<Entrepot>> body = result.successes().stream()
                .map(success -> new CountryGroup<>(success.codePays(), success.nomPays(), success.data()))
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }

    @GetMapping("/{codePays}/{id}")
    public Entrepot getEntrepot(@PathVariable String codePays, @PathVariable Long id) {
        return entrepotAggregationService.getById(codePays, id);
    }
}
