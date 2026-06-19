package com.futurekawa.backendlocal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futurekawa.backendlocal.dto.response.PaysResponse;
import com.futurekawa.backendlocal.service.PaysService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pays")
@RequiredArgsConstructor
public class PaysController {

    private final PaysService paysService;

    @GetMapping
    public PaysResponse getPays() {
        return paysService.getCountryInfo();
    }
}
