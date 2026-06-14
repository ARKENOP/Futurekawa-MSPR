package com.futurekawa.backendlocal.controller;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendlocal.dto.request.UpdateAlerteRequest;
import com.futurekawa.backendlocal.dto.response.AlerteResponse;
import com.futurekawa.backendlocal.service.AlerteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/alertes")
@RequiredArgsConstructor
public class AlerteController {

    private final AlerteService alerteService;

    @GetMapping
    public Page<AlerteResponse> listAlertes(@ParameterObject Pageable pageable) {
        return alerteService.listAll(pageable);
    }

    @GetMapping("/{id}")
    public AlerteResponse getAlerte(@PathVariable Long id) {
        return alerteService.getById(id);
    }

    @PatchMapping("/{id}")
    public AlerteResponse closeAlerte(@PathVariable Long id, @Valid @RequestBody UpdateAlerteRequest request) {
        return alerteService.closeAlerte(id, request);
    }
}
