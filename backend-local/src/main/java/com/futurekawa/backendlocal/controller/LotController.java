package com.futurekawa.backendlocal.controller;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendlocal.dto.request.CreateLotRequest;
import com.futurekawa.backendlocal.dto.request.UpdateLotRequest;
import com.futurekawa.backendlocal.dto.response.LotResponse;
import com.futurekawa.backendlocal.service.LotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @GetMapping
    public Page<LotResponse> listLots(@ParameterObject Pageable pageable) {
        return lotService.listAll(pageable);
    }

    @GetMapping("/{id}")
    public LotResponse getLot(@PathVariable Long id) {
        return lotService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse createLot(@Valid @RequestBody CreateLotRequest request) {
        return lotService.createLot(request);
    }

    @PatchMapping("/{id}")
    public LotResponse updateLotStatut(@PathVariable Long id, @Valid @RequestBody UpdateLotRequest request) {
        return lotService.updateStatut(id, request);
    }
}
