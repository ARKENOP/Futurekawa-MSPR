package com.futurekawa.backendlocal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendlocal.dto.response.EntrepotResponse;
import com.futurekawa.backendlocal.service.EntrepotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/entrepots")
@RequiredArgsConstructor
public class EntrepotController {

    private final EntrepotService entrepotService;

    @GetMapping
    public List<EntrepotResponse> listEntrepots(@RequestParam(required = false) Long exploitationId) {
        if (exploitationId != null) {
            return entrepotService.listByExploitation(exploitationId);
        }
        return entrepotService.listAll();
    }

    @GetMapping("/{id}")
    public EntrepotResponse getEntrepot(@PathVariable Long id) {
        return entrepotService.getById(id);
    }
}
