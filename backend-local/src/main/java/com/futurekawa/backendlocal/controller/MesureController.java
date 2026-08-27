package com.futurekawa.backendlocal.controller;

import java.time.LocalDateTime;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendlocal.service.MesureService;
import com.futurekawa.lib.dto.response.MesureStockageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/entrepots/{entrepotId}/mesures")
@RequiredArgsConstructor
public class MesureController {

    private final MesureService mesureService;

    @GetMapping
    public Page<MesureStockageResponse> getMesuresHistory(
            @PathVariable Long entrepotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject Pageable pageable) {
        return mesureService.getHistoryByEntrepot(entrepotId, from, to, pageable);
    }

    @GetMapping("/latest")
    public MesureStockageResponse getLatestMesure(@PathVariable Long entrepotId) {
        return mesureService.getLatestByEntrepot(entrepotId);
    }
}
