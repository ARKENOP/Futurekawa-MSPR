package com.futurekawa.backendcentral.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.backendcentral.dto.response.MesureStockage;
import com.futurekawa.backendcentral.service.MesureAggregationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/entrepots/{codePays}/{id}/mesures")
@RequiredArgsConstructor
public class MesureController {

    private final MesureAggregationService mesureAggregationService;

    @GetMapping
    public PageDto<MesureStockage> getMesuresHistory(
            @PathVariable String codePays,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return mesureAggregationService.getHistory(codePays, id, from, to, page, size);
    }

    @GetMapping("/latest")
    public MesureStockage getLatestMesure(@PathVariable String codePays, @PathVariable Long id) {
        return mesureAggregationService.getLatest(codePays, id);
    }
}
