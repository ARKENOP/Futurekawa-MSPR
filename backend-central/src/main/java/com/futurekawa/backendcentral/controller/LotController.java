package com.futurekawa.backendcentral.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.dto.envelope.CountryPageGroup;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.CreateLotRequest;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.LotAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.enums.StatutLot;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotAggregationService lotAggregationService;

    @GetMapping
    public ResponseEntity<List<CountryPageGroup<LotResponse>>> listLots(
            @RequestParam(required = false) StatutLot statutLot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FanoutResult<PageDto<LotResponse>> result = lotAggregationService.listAll(statutLot, page, size);
        List<CountryPageGroup<LotResponse>> body = result.successes().stream()
                .map(success -> new CountryPageGroup<>(success.codePays(), success.nomPays(), success.data()))
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }

    @GetMapping("/{codePays}/{id}")
    public LotResponse getLot(@PathVariable String codePays, @PathVariable Long id) {
        return lotAggregationService.getById(codePays, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse createLot(@Valid @RequestBody CreateLotRequest request) {
        return lotAggregationService.create(request);
    }

    @PatchMapping("/{codePays}/{id}")
    public LotResponse updateLotStatut(@PathVariable String codePays, @PathVariable Long id,
                                @Valid @RequestBody UpdateLotRequest request) {
        return lotAggregationService.updateStatut(codePays, id, request);
    }
}
