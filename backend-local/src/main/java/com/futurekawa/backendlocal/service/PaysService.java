package com.futurekawa.backendlocal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.PaysMapper;
import com.futurekawa.backendlocal.repository.PaysRepository;
import com.futurekawa.lib.dto.response.PaysResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaysService {
    private final PaysRepository paysRepository;
    private final PaysMapper paysMapper;
    private final PaysProperties paysProperties;

    public PaysResponse getCountryInfo() {
        return paysRepository.findByCodePays(paysProperties.code())
                .map(paysMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Country not initialized: " + paysProperties.code()));
    }
}
