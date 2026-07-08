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

import com.futurekawa.backendcentral.dto.enums.StatutLot;
import com.futurekawa.backendcentral.dto.envelope.CountryPageGroup;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.request.CreateLotRequest;
import com.futurekawa.backendcentral.dto.request.UpdateLotRequest;
import com.futurekawa.backendcentral.dto.response.Lot;
import com.futurekawa.backendcentral.fanout.FanoutResult;
import com.futurekawa.backendcentral.service.LotAggregationService;
import com.futurekawa.backendcentral.util.ResponseHeaders;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotAggregationService lotAggregationService;

    @GetMapping
    public ResponseEntity<List<CountryPageGroup<Lot>>> listLots(
            @RequestParam(required = false) StatutLot statutLot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FanoutResult<PageDto<Lot>> result = lotAggregationService.listAll(statutLot, page, size);
        List<CountryPageGroup<Lot>> body = result.successes().stream()
                .map(success -> new CountryPageGroup<>(success.codePays(), success.nomPays(), success.data()))
                .toList();
        return ResponseHeaders.withUnavailable(result.unavailable()).body(body);
    }

    @GetMapping("/{codePays}/{id}")
    public Lot getLot(@PathVariable String codePays, @PathVariable Long id) {
        return lotAggregationService.getById(codePays, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Lot createLot(@Valid @RequestBody CreateLotRequest request) {
        return lotAggregationService.create(request);
    }

    @PatchMapping("/{codePays}/{id}")
    public Lot updateLotStatut(@PathVariable String codePays, @PathVariable Long id,
                                @Valid @RequestBody UpdateLotRequest request) {
        return lotAggregationService.updateStatut(codePays, id, request);
    }
}
